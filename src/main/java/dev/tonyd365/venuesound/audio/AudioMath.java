package dev.tonyd365.venuesound.audio;

import java.util.List;

/** Small PCM helpers for 16-bit mono frames. */
public final class AudioMath {

    private AudioMath() {
    }

    /** Returns a new frame with every sample multiplied by {@code gain}, clipped to 16 bit. */
    public static short[] applyGain(short[] frame, float gain) {
        short[] out = new short[frame.length];
        for (int i = 0; i < frame.length; i++) {
            out[i] = clip(Math.round(frame[i] * gain));
        }
        return out;
    }

    /** Sums several frames sample by sample, clipped to 16 bit. */
    public static short[] mix(List<short[]> frames) {
        if (frames.size() == 1) {
            return frames.get(0);
        }
        int length = 0;
        for (short[] f : frames) {
            length = Math.max(length, f.length);
        }
        int[] acc = new int[length];
        for (short[] f : frames) {
            for (int i = 0; i < f.length; i++) {
                acc[i] += f[i];
            }
        }
        short[] out = new short[length];
        for (int i = 0; i < length; i++) {
            out[i] = clip(acc[i]);
        }
        return out;
    }

    private static short clip(int value) {
        if (value > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        if (value < Short.MIN_VALUE) {
            return Short.MIN_VALUE;
        }
        return (short) value;
    }
}
