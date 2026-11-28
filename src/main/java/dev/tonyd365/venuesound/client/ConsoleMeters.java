package dev.tonyd365.venuesound.client;

import net.minecraft.core.BlockPos;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The live levels the server sends for each console, with meter ballistics: a level jumps up at
 * once and falls back at about 20 dB per second; PEAK LEDs stay lit for half a second.
 */
public final class ConsoleMeters {

    /** How fast the meter falls, in dB per second. */
    private static final double FALL_DB_PER_S = 20.0;
    private static final long PEAK_HOLD_MS = 500;
    /** Strip level (0-1) that lights PEAK: about -1 dBFS, near clipping. */
    public static final float PEAK_LEVEL = 0.89F;

    private static final class Levels {
        final float[] value;
        final long[] at;
        final long[] peakUntil;

        Levels(int n) {
            value = new float[n];
            at = new long[n];
            peakUntil = new long[n];
        }
    }

    private static final Map<BlockPos, Levels> LEVELS = new ConcurrentHashMap<>();

    private ConsoleMeters() {
    }

    /** New levels from the server: [0] MAIN L, [1] MAIN R, [2 + strip] inputs, each 0-1. */
    public static void update(BlockPos console, float[] levels) {
        Levels l = LEVELS.computeIfAbsent(console.immutable(), k -> new Levels(levels.length));
        long now = System.currentTimeMillis();
        for (int i = 0; i < Math.min(levels.length, l.value.length); i++) {
            if (levels[i] >= current(l, i, now)) {
                l.value[i] = levels[i];
                l.at[i] = now;
            }
            if (levels[i] >= PEAK_LEVEL) {
                l.peakUntil[i] = now + PEAK_HOLD_MS;
            }
        }
    }

    private static float current(Levels l, int i, long now) {
        if (l.value[i] <= 0) {
            return 0;
        }
        double fallen = FALL_DB_PER_S * (now - l.at[i]) / 1000.0;
        return (float) (l.value[i] * Math.pow(10, -fallen / 20.0));
    }

    /** MAIN L (side 0) or MAIN R (side 1) level now, 0-1. */
    public static float main(BlockPos console, int side) {
        Levels l = LEVELS.get(console);
        return l == null ? 0 : current(l, side, System.currentTimeMillis());
    }

    /** Whether the strip's PEAK LED is lit. */
    public static boolean peak(BlockPos console, int strip) {
        Levels l = LEVELS.get(console);
        return l != null && 2 + strip < l.peakUntil.length && l.peakUntil[2 + strip] > System.currentTimeMillis();
    }

    /**
     * How many of the meter's 8 LEDs a level lights: from -42 dBFS (one LED) in 6 dB steps to
     * -3 dBFS (all eight, the red one).
     */
    public static int leds(float level) {
        if (level <= 0) {
            return 0;
        }
        double db = 20 * Math.log10(level);
        double[] steps = {-42, -36, -30, -24, -18, -12, -6, -3};
        int n = 0;
        for (double s : steps) {
            if (db >= s) {
                n++;
            }
        }
        return n;
    }
}
