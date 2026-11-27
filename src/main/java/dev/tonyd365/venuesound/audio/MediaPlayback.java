package dev.tonyd365.venuesound.audio;

import dev.tonyd365.venuesound.VenueSound;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Plays media deck tracks: decodes files from {@code <game dir>/venuesound_media/} on a worker
 * thread, then feeds 20 ms frames into the cable graph in real time.
 */
public final class MediaPlayback {

    public static final int FRAME = 960; // 20 ms at 48 kHz

    private static final MediaPlayback INSTANCE = new MediaPlayback();

    public static MediaPlayback get() {
        return INSTANCE;
    }

    private final Map<DimPos, Session> sessions = new ConcurrentHashMap<>();
    private final ExecutorService decoder = Executors.newSingleThreadExecutor(r -> daemon(r, "VenueSound-MediaDecode"));
    private final ScheduledExecutorService clock = Executors.newSingleThreadScheduledExecutor(r -> daemon(r, "VenueSound-MediaClock"));

    private MediaPlayback() {
        clock.scheduleAtFixedRate(this::tick, 20, 20, TimeUnit.MILLISECONDS);
    }

    private static Thread daemon(Runnable r, String name) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        return t;
    }

    public static Path mediaFolder() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("venuesound_media");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            VenueSound.LOGGER.warn("Could not create {}", dir, e);
        }
        return dir;
    }

    /** Supported audio files in the media folder, sorted by name. */
    public static List<String> listTracks() {
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.list(mediaFolder())) {
            files.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(MediaDecoder::isSupported)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(names::add);
        } catch (IOException e) {
            VenueSound.LOGGER.warn("Could not list media folder", e);
        }
        return names;
    }

    public boolean isPlaying(DimPos deck) {
        return sessions.containsKey(deck);
    }

    public void play(DimPos deck, String track, boolean loop) {
        stop(deck);
        Path folder = mediaFolder();
        Path file = folder.resolve(track).normalize();
        if (!file.startsWith(folder) || !Files.isRegularFile(file)) {
            VenueSound.LOGGER.warn("Media deck track not found: {}", track);
            return;
        }
        Session session = new Session(UUID.nameUUIDFromBytes(("venuesound:deck:" + deck).getBytes()), loop);
        sessions.put(deck, session);
        decoder.submit(() -> {
            try {
                session.pcm = MediaDecoder.decode(file);
            } catch (Exception e) {
                VenueSound.LOGGER.error("Could not decode {}", file.getFileName(), e);
                sessions.remove(deck, session);
            }
        });
    }

    public void stop(DimPos deck) {
        sessions.remove(deck);
    }

    public void setLoop(DimPos deck, boolean loop) {
        Session s = sessions.get(deck);
        if (s != null) {
            s.loop = loop;
        }
    }

    public void stopAll() {
        sessions.clear();
    }

    private void tick() {
        try {
            for (Map.Entry<DimPos, Session> e : sessions.entrySet()) {
                Session s = e.getValue();
                short[] pcm = s.pcm;
                if (pcm == null) {
                    continue; // still decoding
                }
                if (s.position >= pcm.length) {
                    if (s.loop && pcm.length > 0) {
                        s.position = 0;
                    } else {
                        sessions.remove(e.getKey(), s);
                        continue;
                    }
                }
                short[] frame = new short[FRAME];
                int n = Math.min(FRAME, pcm.length - s.position);
                System.arraycopy(pcm, s.position, frame, 0, n);
                s.position += n;
                SoundRouter.get().routeMedia(e.getKey(), s.stream, frame);
            }
        } catch (Exception ex) {
            VenueSound.LOGGER.error("Media playback tick failed", ex);
        }
    }

    private static final class Session {
        final UUID stream;
        volatile boolean loop;
        volatile short[] pcm;
        int position;

        Session(UUID stream, boolean loop) {
            this.stream = stream;
            this.loop = loop;
        }
    }
}
