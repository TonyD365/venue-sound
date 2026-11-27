package dev.tonyd365.venuesound.audio;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.VenueSoundVoicechatPlugin;
import dev.tonyd365.venuesound.item.WirelessMicItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The cable graph: sources -> (outlets) -> mixer -> (outlets) -> amplifier -> (outlets) -> speakers.
 *
 * Block entities publish immutable {@link RouterState.Node} snapshots from the server thread.
 * Audio from voice chat and media decks is routed on other threads using only those snapshots.
 */
public final class SoundRouter {

    /** How far a wireless mic reaches its receivers, in blocks. */
    public static final double WIRELESS_RANGE = 128.0D;
    /** How close a player must stand to a stand mic to be picked up, in blocks. */
    public static final double STAND_MIC_RANGE = 3.0D;
    /** Longest chain of outlets followed before giving up (also stops cable loops). */
    private static final int MAX_HOPS = 32;

    private static final SoundRouter INSTANCE = new SoundRouter();

    public static SoundRouter get() {
        return INSTANCE;
    }

    private final Map<DimPos, RouterState.Node> nodes = new ConcurrentHashMap<>();
    private final Map<DimPos, SpeakerOutput> outputs = new ConcurrentHashMap<>();
    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();

    private SoundRouter() {
    }

    // ------------------------------------------------------------------ registry (server thread)

    public void put(ServerLevel level, RouterState.Node node) {
        RouterState.Node previous = nodes.put(node.pos(), node);
        if (node.kind() == NodeKind.SPEAKER) {
            SpeakerOutput output = outputs.get(node.pos());
            if (output == null) {
                outputs.put(node.pos(), new SpeakerOutput(level, node.pos(), node.range(), node.heightOff()));
            } else if (previous != null && previous.range() != node.range()) {
                output.setRange(node.range());
            }
        }
    }

    public void remove(DimPos pos) {
        nodes.remove(pos);
        SpeakerOutput output = outputs.remove(pos);
        if (output != null) {
            output.close();
        }
    }

    @Nullable
    public RouterState.Node node(DimPos pos) {
        return nodes.get(pos);
    }

    public void clear() {
        nodes.clear();
        outputs.values().forEach(SpeakerOutput::close);
        outputs.clear();
        decoders.values().forEach(OpusDecoder::close);
        decoders.clear();
    }

    /** Speakers that the given mixer can currently reach. */
    public int countSpeakers(DimPos mixer) {
        RouterState.Node node = nodes.get(mixer);
        if (node == null) {
            return 0;
        }
        Set<DimPos> speakers = new HashSet<>();
        collectSpeakers(node, speakers, new HashSet<>(), 0);
        return speakers.size();
    }

    private void collectSpeakers(RouterState.Node from, Set<DimPos> found, Set<DimPos> visited, int depth) {
        if (depth > MAX_HOPS || !visited.add(from.pos())) {
            return;
        }
        for (DimPos out : from.outputs()) {
            RouterState.Node next = nodes.get(out);
            if (next == null) {
                continue;
            }
            if (next.kind() == NodeKind.SPEAKER) {
                found.add(next.pos());
            } else if (next.kind() == NodeKind.OUTLET || next.kind() == NodeKind.AMPLIFIER) {
                collectSpeakers(next, found, visited, depth + 1);
            }
        }
    }

    // ------------------------------------------------------------------ voice chat (voice thread)

    public void onMicrophonePacket(MicrophonePacketEvent event) {
        try {
            routeMicrophone(event);
        } catch (Exception e) {
            VenueSound.LOGGER.error("Failed to route microphone packet", e);
        }
    }

    private void routeMicrophone(MicrophonePacketEvent event) {
        VoicechatServerApi api = VenueSoundVoicechatPlugin.serverApi();
        VoicechatConnection connection = event.getSenderConnection();
        if (api == null || connection == null || nodes.isEmpty()) {
            return;
        }
        if (!(connection.getPlayer().getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        UUID talker = player.getUUID();
        byte[] opus = event.getPacket().getOpusEncodedData();
        OpusDecoder decoder = decoders.computeIfAbsent(talker, k -> api.createDecoder());
        if (opus == null || opus.length == 0) {
            decoder.resetState();
            return;
        }

        int micChannel = WirelessMicItem.activeChannel(player);
        List<RouterState.Node> sources = new ArrayList<>();
        double wirelessSqr = WIRELESS_RANGE * WIRELESS_RANGE;
        double standSqr = STAND_MIC_RANGE * STAND_MIC_RANGE;
        for (RouterState.Node node : nodes.values()) {
            if (!node.pos().dimension().equals(player.level().dimension())) {
                continue;
            }
            double distSqr = node.pos().distanceSqrTo(player.getX(), player.getY(), player.getZ());
            if (node.kind() == NodeKind.RECEIVER && micChannel > 0 && node.channel() == micChannel && distSqr <= wirelessSqr) {
                sources.add(node);
            } else if (node.kind() == NodeKind.STAND_MIC && distSqr <= standSqr) {
                sources.add(node);
            }
        }
        if (sources.isEmpty()) {
            return;
        }
        short[] pcm = decoder.decode(opus);
        Map<DimPos, List<short[]>> perSpeaker = new HashMap<>();
        for (RouterState.Node source : sources) {
            routeFrom(source, pcm, perSpeaker);
        }
        deliver(talker, perSpeaker);
    }

    // ------------------------------------------------------------------ media decks (media thread)

    public void routeMedia(DimPos deck, UUID stream, short[] pcm) {
        RouterState.Node node = nodes.get(deck);
        if (node == null) {
            return;
        }
        Map<DimPos, List<short[]>> perSpeaker = new HashMap<>();
        routeFrom(node, pcm, perSpeaker);
        deliver(stream, perSpeaker);
    }

    // ------------------------------------------------------------------ graph walk

    /** Follows a source's cables to every mixer, applies that input's fader, then continues to the speakers. */
    private void routeFrom(RouterState.Node source, short[] pcm, Map<DimPos, List<short[]>> perSpeaker) {
        walk(source, 1F, pcm, perSpeaker, new HashSet<>(), 0, false);
    }

    /**
     * @param from  device whose outputs we follow; a mixer input is matched against this device
     * @param mixed whether the signal has already passed a mixer
     */
    private void walk(RouterState.Node from, float gain, short[] pcm,
                      Map<DimPos, List<short[]>> perSpeaker, Set<DimPos> visited, int depth, boolean mixed) {
        if (depth > MAX_HOPS || !visited.add(from.pos())) {
            return;
        }
        for (DimPos outPos : from.outputs()) {
            RouterState.Node next = nodes.get(outPos);
            if (next == null) {
                continue;
            }
            switch (next.kind()) {
                case OUTLET -> walk(next, gain, pcm, perSpeaker, visited, depth + 1, mixed);
                case MIXER -> {
                    if (mixed || next.mixer() == null) {
                        continue;
                    }
                    float g = next.mixer().gainFor(from.pos());
                    if (g > 0F) {
                        walk(next, gain * g, pcm, perSpeaker, visited, depth + 1, true);
                    }
                }
                case AMPLIFIER -> {
                    if (mixed && next.ampLevel() > 0F) {
                        walk(next, gain * next.ampLevel(), pcm, perSpeaker, visited, depth + 1, true);
                    }
                }
                case SPEAKER -> {
                    if (mixed && gain > 0F) {
                        perSpeaker.computeIfAbsent(next.pos(), k -> new ArrayList<>()).add(AudioMath.applyGain(pcm, gain));
                    }
                }
                default -> {
                }
            }
        }
    }

    private void deliver(UUID stream, Map<DimPos, List<short[]>> perSpeaker) {
        for (Map.Entry<DimPos, List<short[]>> e : perSpeaker.entrySet()) {
            SpeakerOutput output = outputs.get(e.getKey());
            if (output != null) {
                output.push(stream, AudioMath.mix(e.getValue()));
            }
        }
    }
}
