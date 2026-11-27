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
 * Holds the snapshots of every device and feeds audio from voice chat and media players through
 * the {@link SignalEngine} to the speakers' voice chat channels.
 */
public final class SoundRouter {

    /** How far a wireless mic reaches its receivers, in blocks. */
    public static final double WIRELESS_RANGE = 128.0D;
    /** How close a player must stand to a stand mic to be picked up, in blocks. */
    public static final double STAND_MIC_RANGE = 3.0D;

    private static final SoundRouter INSTANCE = new SoundRouter();

    public static SoundRouter get() {
        return INSTANCE;
    }

    private final Map<DimPos, RouterState.Node> nodes = new ConcurrentHashMap<>();
    private final Map<DimPos, SpeakerOutput> outputs = new ConcurrentHashMap<>();
    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
    private final SignalEngine<RouterState.Node> engine = new SignalEngine<>();

    /** The engine's view of the snapshots. */
    private final SignalEngine.Graph<RouterState.Node> graph = new SignalEngine.Graph<>() {
        @Override
        public NodeKind kind(RouterState.Node node) {
            return node.kind();
        }

        @Override
        public List<SignalEngine.Edge<RouterState.Node>> outputs(RouterState.Node node) {
            List<SignalEngine.Edge<RouterState.Node>> edges = new ArrayList<>(node.outputs().size());
            for (RouterState.Out o : node.outputs()) {
                RouterState.Node target = nodes.get(o.target());
                if (target != null) {
                    edges.add(new SignalEngine.Edge<>(target, o.fromPort(), o.toPort()));
                }
            }
            return edges;
        }

        @Override
        public float param(RouterState.Node console, String key) {
            return console.mixer() == null ? 0F : console.mixer().get(key);
        }

        @Override
        public int inputSlot(RouterState.Node console, RouterState.Node upstream) {
            return console.mixer() == null ? -1 : console.mixer().slotOf(upstream.pos());
        }

        @Override
        public float ampLevel(RouterState.Node amp) {
            return amp.ampLevel();
        }

        @Override
        public boolean speakerOn(RouterState.Node speaker) {
            return speaker.range() > 0;
        }

        @Override
        public Object id(RouterState.Node node) {
            return node.pos();
        }
    };

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
        engine.clear();
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
        Map<DimPos, List<float[]>> perSpeaker = new HashMap<>();
        for (RouterState.Node source : sources) {
            engine.route(graph, source, talker, pcm, collect(perSpeaker));
        }
        deliver(talker, perSpeaker);
    }

    // ------------------------------------------------------------------ media (media thread)

    public void routeMedia(DimPos deck, UUID stream, short[] pcm) {
        RouterState.Node node = nodes.get(deck);
        if (node == null) {
            return;
        }
        Map<DimPos, List<float[]>> perSpeaker = new HashMap<>();
        engine.route(graph, node, stream, pcm, collect(perSpeaker));
        deliver(stream, perSpeaker);
    }

    public void routeIntoConsole(DimPos console, int slot, UUID stream, short[] pcm) {
        RouterState.Node node = nodes.get(console);
        if (node == null || node.kind() != NodeKind.MIXER) {
            return;
        }
        Map<DimPos, List<float[]>> perSpeaker = new HashMap<>();
        engine.routeIntoConsole(graph, node, slot, stream, pcm, collect(perSpeaker));
        deliver(stream, perSpeaker);
    }

    private static SignalEngine.Sink<RouterState.Node> collect(Map<DimPos, List<float[]>> perSpeaker) {
        return (speaker, frame) -> perSpeaker.computeIfAbsent(speaker.pos(), k -> new ArrayList<>()).add(frame);
    }

    private void deliver(UUID stream, Map<DimPos, List<float[]>> perSpeaker) {
        for (Map.Entry<DimPos, List<float[]>> e : perSpeaker.entrySet()) {
            SpeakerOutput output = outputs.get(e.getKey());
            if (output == null) {
                continue;
            }
            List<float[]> frames = e.getValue();
            float[] sum = new float[frames.get(0).length];
            for (float[] f : frames) {
                for (int i = 0; i < sum.length && i < f.length; i++) {
                    sum[i] += f[i];
                }
            }
            output.push(stream, ConsoleDsp.toPcm(sum));
        }
    }

    /** Speakers a device currently reaches, for display. */
    public int countSpeakers(DimPos from) {
        RouterState.Node node = nodes.get(from);
        if (node == null) {
            return 0;
        }
        Set<DimPos> found = new HashSet<>();
        countSpeakers(node, found, new HashSet<>(), 0);
        return found.size();
    }

    private void countSpeakers(RouterState.Node from, Set<DimPos> found, Set<DimPos> visited, int depth) {
        if (depth > 32 || !visited.add(from.pos())) {
            return;
        }
        for (RouterState.Out o : from.outputs()) {
            RouterState.Node next = nodes.get(o.target());
            if (next == null) {
                continue;
            }
            if (next.kind() == NodeKind.SPEAKER) {
                found.add(next.pos());
            } else {
                countSpeakers(next, found, visited, depth + 1);
            }
        }
    }
}
