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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The signal chain: wireless mic -> receiver -> mixer -> speakers.
 *
 * Block entities publish immutable snapshots here from the server thread; microphone packets are
 * routed on Simple Voice Chat's thread using only those snapshots.
 */
public final class SoundRouter {

    /** How far a wireless mic reaches its receivers, in blocks. */
    public static final double WIRELESS_RANGE = 128.0D;

    private static final SoundRouter INSTANCE = new SoundRouter();

    public static SoundRouter get() {
        return INSTANCE;
    }

    private final Map<DimPos, RouterState.Receiver> receivers = new ConcurrentHashMap<>();
    private final Map<DimPos, RouterState.Mixer> mixers = new ConcurrentHashMap<>();
    private final Map<DimPos, RouterState.Speaker> speakers = new ConcurrentHashMap<>();
    private final Map<DimPos, SpeakerOutput> outputs = new ConcurrentHashMap<>();
    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();

    private SoundRouter() {
    }

    // ------------------------------------------------------------------ registry (server thread)

    public void putReceiver(RouterState.Receiver receiver) {
        receivers.put(receiver.pos(), receiver);
    }

    public void putMixer(RouterState.Mixer mixer) {
        mixers.put(mixer.pos(), mixer);
    }

    public void putSpeaker(ServerLevel level, RouterState.Speaker speaker) {
        RouterState.Speaker previous = speakers.put(speaker.pos(), speaker);
        SpeakerOutput output = outputs.get(speaker.pos());
        if (output == null) {
            outputs.put(speaker.pos(), new SpeakerOutput(level, speaker.pos(), speaker.range()));
        } else if (previous != null && previous.range() != speaker.range()) {
            output.setRange(speaker.range());
        }
    }

    public void remove(DimPos pos) {
        receivers.remove(pos);
        mixers.remove(pos);
        speakers.remove(pos);
        SpeakerOutput output = outputs.remove(pos);
        if (output != null) {
            output.close();
        }
    }

    public void clear() {
        receivers.clear();
        mixers.clear();
        speakers.clear();
        outputs.values().forEach(SpeakerOutput::close);
        outputs.clear();
        decoders.values().forEach(OpusDecoder::close);
        decoders.clear();
    }

    /** Number of speakers currently patched to the mixer at {@code mixer}. */
    public int countSpeakers(DimPos mixer) {
        int n = 0;
        for (RouterState.Speaker s : speakers.values()) {
            if (mixer.equals(s.mixer())) {
                n++;
            }
        }
        return n;
    }

    /** Receiver channels (1-8) currently patched to the mixer at {@code mixer}. */
    public Set<Integer> patchedChannels(DimPos mixer) {
        Set<Integer> channels = new HashSet<>();
        for (RouterState.Receiver r : receivers.values()) {
            if (mixer.equals(r.mixer())) {
                channels.add(r.channel());
            }
        }
        return channels;
    }

    // ------------------------------------------------------------------ audio (voice thread)

    public void onMicrophonePacket(MicrophonePacketEvent event) {
        try {
            route(event);
        } catch (Exception e) {
            VenueSound.LOGGER.error("Failed to route microphone packet", e);
        }
    }

    private void route(MicrophonePacketEvent event) {
        VoicechatServerApi api = VenueSoundVoicechatPlugin.serverApi();
        VoicechatConnection connection = event.getSenderConnection();
        if (api == null || connection == null || receivers.isEmpty()) {
            return;
        }
        if (!(connection.getPlayer().getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        int channel = WirelessMicItem.activeChannel(player);
        UUID talker = player.getUUID();
        byte[] opus = event.getPacket().getOpusEncodedData();

        OpusDecoder decoder = decoders.computeIfAbsent(talker, k -> api.createDecoder());
        if (opus == null || opus.length == 0) {
            decoder.resetState();
            return;
        }
        if (channel <= 0) {
            return;
        }

        Set<DimPos> reachedMixers = new HashSet<>();
        double maxSqr = WIRELESS_RANGE * WIRELESS_RANGE;
        for (RouterState.Receiver receiver : receivers.values()) {
            if (receiver.channel() != channel || receiver.mixer() == null) {
                continue;
            }
            if (!receiver.pos().dimension().equals(player.level().dimension())) {
                continue;
            }
            if (receiver.pos().distanceSqrTo(player.getX(), player.getY(), player.getZ()) > maxSqr) {
                continue;
            }
            reachedMixers.add(receiver.mixer());
        }
        if (reachedMixers.isEmpty()) {
            return;
        }

        short[] pcm = decoder.decode(opus);
        for (DimPos mixerPos : reachedMixers) {
            RouterState.Mixer mixer = mixers.get(mixerPos);
            if (mixer == null) {
                continue;
            }
            float gain = mixer.effectiveGain(channel);
            if (gain <= 0F) {
                continue;
            }
            short[] frame = AudioMath.applyGain(pcm, gain);
            for (RouterState.Speaker speaker : speakers.values()) {
                if (!mixerPos.equals(speaker.mixer())) {
                    continue;
                }
                SpeakerOutput output = outputs.get(speaker.pos());
                if (output != null) {
                    output.push(talker, frame);
                }
            }
        }
    }
}
