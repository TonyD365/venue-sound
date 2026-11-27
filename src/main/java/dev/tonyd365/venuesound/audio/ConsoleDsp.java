package dev.tonyd365.venuesound.audio;

/**
 * The console's signal processing for one input strip: 3-band EQ (low shelf 120 Hz, peak 1 kHz,
 * high shelf 8 kHz, +/-12 dB) and, per console, the effects unit (a feedback delay).
 * Pure Java, shared with the Workbench.
 */
public final class ConsoleDsp {

    private static final double RATE = MediaDecoder.SAMPLE_RATE;

    private ConsoleDsp() {
    }

    /** One biquad filter with its own memory. */
    public static final class Biquad {
        private double b0, b1, b2, a1, a2;
        private double x1, x2, y1, y2;
        private double lastGain = Double.NaN;

        /** type: 0 low shelf, 1 peaking, 2 high shelf (RBJ audio EQ cookbook). */
        void set(int type, double freq, double gainDb, double q) {
            if (gainDb == lastGain) {
                return;
            }
            lastGain = gainDb;
            double a = Math.pow(10, gainDb / 40);
            double w = 2 * Math.PI * freq / RATE;
            double cos = Math.cos(w), sin = Math.sin(w);
            double alpha = sin / (2 * q);
            double nb0, nb1, nb2, na0, na1, na2;
            if (type == 1) {
                nb0 = 1 + alpha * a;
                nb1 = -2 * cos;
                nb2 = 1 - alpha * a;
                na0 = 1 + alpha / a;
                na1 = -2 * cos;
                na2 = 1 - alpha / a;
            } else {
                double sq = 2 * Math.sqrt(a) * alpha;
                if (type == 0) {
                    nb0 = a * ((a + 1) - (a - 1) * cos + sq);
                    nb1 = 2 * a * ((a - 1) - (a + 1) * cos);
                    nb2 = a * ((a + 1) - (a - 1) * cos - sq);
                    na0 = (a + 1) + (a - 1) * cos + sq;
                    na1 = -2 * ((a - 1) + (a + 1) * cos);
                    na2 = (a + 1) + (a - 1) * cos - sq;
                } else {
                    nb0 = a * ((a + 1) + (a - 1) * cos + sq);
                    nb1 = -2 * a * ((a - 1) + (a + 1) * cos);
                    nb2 = a * ((a + 1) + (a - 1) * cos - sq);
                    na0 = (a + 1) - (a - 1) * cos + sq;
                    na1 = 2 * ((a - 1) - (a + 1) * cos);
                    na2 = (a + 1) - (a - 1) * cos - sq;
                }
            }
            b0 = nb0 / na0;
            b1 = nb1 / na0;
            b2 = nb2 / na0;
            a1 = na1 / na0;
            a2 = na2 / na0;
        }

        double process(double x) {
            double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }
    }

    /** EQ memory for one stream on one strip. */
    public static final class Eq {
        private final Biquad low = new Biquad();
        private final Biquad mid = new Biquad();
        private final Biquad high = new Biquad();

        /** Returns a new frame: EQ'd and multiplied by {@code gain}. */
        public float[] process(short[] in, float lowDb, float midDb, float highDb, float gain) {
            low.set(0, 120, lowDb, 0.707);
            mid.set(1, 1000, midDb, 0.9);
            high.set(2, 8000, highDb, 0.707);
            boolean flat = lowDb == 0 && midDb == 0 && highDb == 0;
            float[] out = new float[in.length];
            for (int i = 0; i < in.length; i++) {
                double x = in[i];
                if (!flat) {
                    x = high.process(mid.process(low.process(x)));
                }
                out[i] = (float) (x * gain);
            }
            return out;
        }
    }

    /** The effects unit: a feedback delay, 80-580 ms, for one stream on one console. */
    public static final class Echo {
        private final float[] buffer = new float[(int) (RATE * 0.6)];
        private int pos;

        public float[] process(float[] send, float time) {
            int delay = (int) (RATE * (0.08 + 0.5 * Math.max(0, Math.min(1, time))));
            float[] out = new float[send.length];
            for (int i = 0; i < send.length; i++) {
                int read = Math.floorMod(pos - delay, buffer.length);
                float wet = buffer[read];
                out[i] = wet;
                buffer[pos] = send[i] + wet * 0.38F;
                pos = (pos + 1) % buffer.length;
            }
            return out;
        }
    }

    public static short[] toPcm(float[] in) {
        short[] out = new short[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(in[i])));
        }
        return out;
    }

    /** Left/right balance for a pan value -1..1 (centre = full level on both sides). */
    public static float panLeft(float pan) {
        return Math.min(1F, 1F - pan);
    }

    public static float panRight(float pan) {
        return Math.min(1F, 1F + pan);
    }
}
