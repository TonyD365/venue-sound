package dev.tonyd365.venuesound.workbench;

import dev.tonyd365.venuesound.audio.MediaDecoder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Plays the scene through your computer: your microphone and the media decks go through the
 * cables, console and amplifiers like in the mod, and every speaker is heard from your position
 * in stereo (louder when close, panned by where it stands).
 */
public final class AudioEngine implements Runnable {

    private static final int RATE = MediaDecoder.SAMPLE_RATE;
    private static final int FRAME = 960; // 20 ms

    private final Scene scene;
    private final Map<Device, short[]> deckPcm = new ConcurrentHashMap<>();
    private final Map<Device, Integer> deckPos = new ConcurrentHashMap<>();
    private final Map<Device, String> deckStatus = new ConcurrentHashMap<>();
    private final Map<Device, Double> meters = new ConcurrentHashMap<>();
    private volatile TargetDataLine mic;
    private volatile String error = "";

    /** Per console: [0] MAIN L, [1] MAIN R, [2 + strip] inputs; level 0-1 and when it was set. */
    private final Map<Device, float[]> consoleLevels = new ConcurrentHashMap<>();
    private final Map<Device, long[]> consoleTimes = new ConcurrentHashMap<>();

    public AudioEngine(Scene scene) {
        this.scene = scene;
        scene.engine.setMeter((console, strip, input, l, r) -> {
            float[] v = consoleLevels.computeIfAbsent(console, k -> new float[22]);
            long[] t = consoleTimes.computeIfAbsent(console, k -> new long[22]);
            long now = System.currentTimeMillis();
            hold(v, t, 0, l, now);
            hold(v, t, 1, r, now);
            if (strip >= 0 && strip < 20) {
                hold(v, t, 2 + strip, input, now);
            }
        });
    }

    private static void hold(float[] v, long[] t, int i, float level, long now) {
        if (level >= fallen(v[i], t[i], now)) {
            v[i] = level;
            t[i] = now;
        }
    }

    /** Meter ballistics: falls about 20 dB per second. */
    private static float fallen(float level, long at, long now) {
        return level <= 0 ? 0 : (float) (level * Math.pow(10, -(20.0 * (now - at) / 1000.0) / 20.0));
    }

    /** A console's MAIN L (side 0) or MAIN R (side 1) level now, 0-1. */
    public float mainLevel(Device console, int side) {
        float[] v = consoleLevels.get(console);
        return v == null ? 0 : fallen(v[side], consoleTimes.get(console)[side], System.currentTimeMillis());
    }

    /** Whether a strip hit about -1 dBFS in the last half second. */
    public boolean peak(Device console, int strip) {
        float[] v = consoleLevels.get(console);
        return v != null && 2 + strip < v.length && v[2 + strip] >= 0.89F
                && System.currentTimeMillis() - consoleTimes.get(console)[2 + strip] < 500;
    }

    /** LEDs lit on an 8-LED meter: -42 dBFS for one, then 6 dB steps, -3 dBFS for all eight. */
    public static int leds(float level) {
        if (level <= 0) {
            return 0;
        }
        double db = 20 * Math.log10(level);
        int n = 0;
        for (double s : new double[] {-42, -36, -30, -24, -18, -12, -6, -3}) {
            if (db >= s) {
                n++;
            }
        }
        return n;
    }

    public void start() {
        Thread t = new Thread(this, "Workbench-Audio");
        t.setDaemon(true);
        t.start();
    }

    public String error() {
        return error;
    }

    public double meter(Device d) {
        return meters.getOrDefault(d, 0.0);
    }

    public String deckStatus(Device d) {
        return deckStatus.getOrDefault(d, "");
    }

    // ------------------------------------------------------------------ microphone

    public synchronized boolean setTalking(boolean talking) {
        scene.talking = talking;
        if (talking && mic == null) {
            try {
                AudioFormat f = new AudioFormat(RATE, 16, 1, true, false);
                TargetDataLine line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, f));
                line.open(f, FRAME * 2 * 4);
                line.start();
                mic = line;
            } catch (Exception e) {
                error = "Microphone unavailable: " + e.getMessage();
                scene.talking = false;
                return false;
            }
        } else if (!talking && mic != null) {
            mic.stop();
            mic.close();
            mic = null;
        }
        return true;
    }

    // ------------------------------------------------------------------ media decks

    public List<String> tracks() {
        List<String> names = new ArrayList<>();
        try (Stream<Path> files = Files.list(scene.mediaFolder)) {
            files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
                    .filter(MediaDecoder::isSupported).sorted(String.CASE_INSENSITIVE_ORDER).forEach(names::add);
        } catch (IOException e) {
            // folder missing: no tracks
        }
        return names;
    }

    /** The deck's front-panel buttons, same behaviour as the block. */
    public void pressDeck(Device deck, String button) {
        List<String> tracks = tracks();
        switch (button) {
            case "prev", "next" -> {
                if (tracks.isEmpty()) {
                    deckStatus.put(deck, "no files in " + scene.mediaFolder.getFileName());
                    return;
                }
                int i = Math.max(0, tracks.indexOf(deck.track));
                if (tracks.contains(deck.track)) {
                    i = Math.floorMod(i + ("next".equals(button) ? 1 : -1), tracks.size());
                }
                deck.track = tracks.get(i);
                if (deck.playing) {
                    load(deck);
                }
            }
            case "play" -> {
                if (deck.track.isEmpty() && !tracks.isEmpty()) {
                    deck.track = tracks.get(0);
                }
                if (deck.track.isEmpty()) {
                    deckStatus.put(deck, "no files in " + scene.mediaFolder.getFileName());
                    return;
                }
                deck.playing = true;
                load(deck);
            }
            case "stop" -> {
                deck.playing = false;
                deckPcm.remove(deck);
                deckStatus.put(deck, "stopped");
            }
            case "loop" -> deck.loop = !deck.loop;
            default -> {
            }
        }
    }

    private void load(Device deck) {
        deckPcm.remove(deck);
        deckPos.put(deck, 0);
        deckStatus.put(deck, "loading…");
        Path file = scene.mediaFolder.resolve(deck.track);
        Thread t = new Thread(() -> {
            try {
                deckPcm.put(deck, MediaDecoder.decode(file));
                deckStatus.put(deck, "playing");
            } catch (Exception e) {
                deck.playing = false;
                deckStatus.put(deck, "can't decode: " + e.getMessage());
            }
        }, "Workbench-Decode");
        t.setDaemon(true);
        t.start();
    }

    /** The console's USB player buttons (prev, play, stop, next). */
    public void pressUsb(Device console, String button) {
        List<String> tracks = tracks();
        switch (button) {
            case "prev", "next" -> {
                if (tracks.isEmpty()) {
                    return;
                }
                int i = Math.max(0, tracks.indexOf(console.usbTrack));
                if (tracks.contains(console.usbTrack)) {
                    i = Math.floorMod(i + ("next".equals(button) ? 1 : -1), tracks.size());
                }
                console.usbTrack = tracks.get(i);
                if (console.usbPlaying) {
                    console.track = console.usbTrack;
                    load(console);
                }
            }
            case "play" -> {
                if (console.usbTrack.isEmpty() && !tracks.isEmpty()) {
                    console.usbTrack = tracks.get(0);
                }
                if (!console.usbTrack.isEmpty()) {
                    console.usbPlaying = true;
                    console.playing = true;
                    console.track = console.usbTrack;
                    load(console);
                }
            }
            case "stop" -> {
                console.usbPlaying = false;
                console.playing = false;
                deckPcm.remove(console);
            }
            default -> {
            }
        }
    }

    private short[] nextDeckFrame(Device deck) {
        short[] pcm = deckPcm.get(deck);
        if (!deck.playing || pcm == null) {
            return null;
        }
        int pos = deckPos.getOrDefault(deck, 0);
        if (pos >= pcm.length) {
            if (deck.loop) {
                pos = 0;
            } else {
                deck.playing = false;
                deckStatus.put(deck, "finished");
                return null;
            }
        }
        short[] frame = new short[FRAME];
        int n = Math.min(FRAME, pcm.length - pos);
        System.arraycopy(pcm, pos, frame, 0, n);
        deckPos.put(deck, pos + n);
        return frame;
    }

    // ------------------------------------------------------------------ the mix

    @Override
    public void run() {
        AudioFormat out = new AudioFormat(RATE, 16, 2, true, false);
        SourceDataLine speakers;
        try {
            speakers = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, out));
            speakers.open(out, FRAME * 4 * 6);
            speakers.start();
        } catch (Exception e) {
            error = "No audio output: " + e.getMessage();
            return;
        }
        byte[] micBytes = new byte[FRAME * 2];
        byte[] outBytes = new byte[FRAME * 4];
        while (true) {
            short[] voice = null;
            TargetDataLine line = mic;
            if (scene.talking && line != null) {
                int read = line.read(micBytes, 0, micBytes.length);
                if (read == micBytes.length) {
                    voice = new short[FRAME];
                    for (int i = 0; i < FRAME; i++) {
                        voice[i] = (short) ((micBytes[2 * i] & 0xFF) | (micBytes[2 * i + 1] << 8));
                    }
                }
            }
            float[] left = new float[FRAME];
            float[] right = new float[FRAME];
            Map<Device, Double> levels = new HashMap<>();
            if (voice != null) {
                for (Device source : scene.voiceSources()) {
                    route(source, voice, left, right, levels);
                }
            }
            for (Device d : scene.devices) {
                if (d.type == Device.Type.MEDIA_DECK) {
                    short[] frame = nextDeckFrame(d);
                    if (frame != null) {
                        route(d, frame, left, right, levels);
                    }
                } else if (d.type.kind == dev.tonyd365.venuesound.audio.NodeKind.MIXER && d.usbPlaying) {
                    short[] frame = nextDeckFrame(d);
                    if (frame == null) {
                        d.usbPlaying = false;
                    } else {
                        routeUsb(d, frame, left, right, levels);
                    }
                }
            }
            for (int i = 0; i < FRAME; i++) {
                int l = clip(left[i]), r = clip(right[i]);
                outBytes[4 * i] = (byte) l;
                outBytes[4 * i + 1] = (byte) (l >> 8);
                outBytes[4 * i + 2] = (byte) r;
                outBytes[4 * i + 3] = (byte) (r >> 8);
            }
            meters.replaceAll((d, v) -> v * 0.8);
            levels.forEach((d, v) -> meters.merge(d, v, Math::max));
            speakers.write(outBytes, 0, outBytes.length); // blocks: this is the clock
        }
    }

    /** Sends one frame from {@code source} through the cables and mixes every reached speaker into the output. */
    private void route(Device source, short[] pcm, float[] left, float[] right, Map<Device, Double> levels) {
        levels.merge(source, peak(pcm), Math::max);
        scene.engine.route(scene.graph, source, source, pcm, (sp, frame) -> hear(sp, frame, left, right, levels));
    }

    private void routeUsb(Device console, short[] pcm, float[] left, float[] right, Map<Device, Double> levels) {
        levels.merge(console, peak(pcm), Math::max);
        scene.engine.routeIntoConsole(scene.graph, console, console.layout().usbStrip, "usb", pcm,
                (sp, frame) -> hear(sp, frame, left, right, levels));
    }

    private static double peak(short[] pcm) {
        double peak = 0;
        for (short s : pcm) {
            peak = Math.max(peak, Math.abs(s) / 32768.0);
        }
        return peak;
    }

    /** One speaker's frame, heard from where you stand. */
    private void hear(Device sp, float[] pcm, float[] left, float[] right, Map<Device, Double> levels) {
        {
            float gain = 1F;
            double dx = sp.x + 0.5 - scene.listenerX;
            double dz = sp.z + 0.5 - scene.listenerZ;
            double dist = Math.hypot(dx, dz);
            double atten = Math.max(0, 1 - dist / sp.range);
            if (atten <= 0) {
                return;
            }
            // you face the stage (north): speakers to the east are on your right
            double pan = dist < 0.01 ? 0 : Math.max(-1, Math.min(1, dx / dist));
            double angle = (pan + 1) * Math.PI / 4;
            float gl = (float) (gain * atten * Math.cos(angle) * Math.sqrt(2) * 0.8);
            float gr = (float) (gain * atten * Math.sin(angle) * Math.sqrt(2) * 0.8);
            for (int i = 0; i < FRAME && i < pcm.length; i++) {
                left[i] += pcm[i] * gl;
                right[i] += pcm[i] * gr;
            }
            float fp = 0;
            for (float f : pcm) {
                fp = Math.max(fp, Math.abs(f) / 32768F);
            }
            levels.merge(sp, Math.min(1, fp * atten), Math::max);
        }
    }

    private static int clip(float v) {
        return (int) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
    }
}
