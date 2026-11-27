package dev.tonyd365.venuesound.audio;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import dev.tonyd365.venuesound.VenueSoundVoicechatPlugin;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The audio stream of one PA speaker. Frames pushed by the voice thread are buffered per talker
 * and pulled by Simple Voice Chat's audio player every 20 ms, mixed into one frame.
 */
public final class SpeakerOutput implements Supplier<short[]> {

    /** Frames to buffer before playback starts, to absorb network jitter (20 ms each). */
    private static final int PREBUFFER = 3;
    /** Upper bound per talker, so a stalled player cannot grow memory without limit. */
    private static final int MAX_QUEUE = 50;

    private final UUID channelId;
    private final ServerLevel level;
    private final DimPos pos;
    private final Map<UUID, ArrayDeque<short[]>> queues = new HashMap<>();
    @Nullable
    private AudioPlayer player;
    private int range;
    private final double heightOff;

    public SpeakerOutput(ServerLevel level, DimPos pos, int range, double heightOff) {
        this.level = level;
        this.pos = pos;
        this.range = range;
        this.heightOff = heightOff;
        this.channelId = UUID.nameUUIDFromBytes(("venuesound:speaker:" + pos.dimension().location() + ":" + pos.pos().asLong()).getBytes());
    }

    public synchronized void setRange(int range) {
        this.range = range;
        if (player != null) {
            player.stopPlaying();
            player = null;
        }
    }

    public synchronized void push(UUID talker, short[] frame) {
        ArrayDeque<short[]> queue = queues.computeIfAbsent(talker, k -> {
            ArrayDeque<short[]> q = new ArrayDeque<>();
            for (int i = 0; i < PREBUFFER; i++) {
                q.add(new short[0]);
            }
            return q;
        });
        if (queue.size() < MAX_QUEUE) {
            queue.add(frame);
        }
        if (player == null || !player.isPlaying()) {
            startPlayer();
        }
    }

    private void startPlayer() {
        VoicechatServerApi api = VenueSoundVoicechatPlugin.serverApi();
        if (api == null) {
            return;
        }
        Position position = api.createPosition(pos.pos().getX() + 0.5D, pos.pos().getY() + heightOff, pos.pos().getZ() + 0.5D);
        LocationalAudioChannel channel = api.createLocationalAudioChannel(channelId, api.fromServerLevel(level), position);
        if (channel == null) {
            return;
        }
        channel.setCategory(VenueSoundVoicechatPlugin.CATEGORY);
        channel.setDistance(range);
        player = api.createAudioPlayer(channel, api.createEncoder(), this);
        player.startPlaying();
    }

    @Override
    public synchronized short[] get() {
        List<short[]> frames = new ArrayList<>();
        Iterator<Map.Entry<UUID, ArrayDeque<short[]>>> it = queues.entrySet().iterator();
        while (it.hasNext()) {
            ArrayDeque<short[]> queue = it.next().getValue();
            short[] frame = queue.poll();
            if (frame == null) {
                it.remove();
                continue;
            }
            if (frame.length > 0) {
                frames.add(frame);
            }
        }
        if (queues.isEmpty()) {
            // Nobody is talking any more: returning null ends this playback session.
            player = null;
            return null;
        }
        if (frames.isEmpty()) {
            return new short[960];
        }
        return AudioMath.mix(frames);
    }

    public synchronized void close() {
        queues.clear();
        if (player != null) {
            player.stopPlaying();
            player = null;
        }
    }
}
