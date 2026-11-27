package dev.tonyd365.venuesound.block;

/**
 * Where every control sits on the two-block console, in panel units (see {@link PanelMath}):
 * u 0-32 across both halves, depth 0-16 from the front edge. Shared by clicks and rendering.
 */
public final class MixerLayout {

    private MixerLayout() {
    }

    public static final double TOP = 7.0;          // height of the control surface
    public static final double BACK_DEPTH = 12.0;  // raised connector panel starts here
    public static final double FADER_MIN = 1.2;    // fader travel, front (0 %) to back (max)
    public static final double FADER_MAX = 7.2;
    public static final double MUTE_MIN = 8.0;
    public static final double MUTE_MAX = 9.1;
    public static final double KNOB_DEPTH = 10.5;
    public static final double KNOB_RADIUS = 0.85;
    public static final double MASTER_U = 28.5;

    /** Centre of channel strip {@code i} (0-7). */
    public static double stripU(int i) {
        return 2.5 + 3.0 * i;
    }

    /** Fader position 0-1 for a click at {@code depth}. */
    public static double faderValue(double depth) {
        return Math.max(0, Math.min(1, (depth - FADER_MIN) / (FADER_MAX - FADER_MIN)));
    }

    public static double faderDepth(double value) {
        return FADER_MIN + value * (FADER_MAX - FADER_MIN);
    }

    /** Which strip a click at {@code u} is on: 0-7, 8 for master, -1 for none. */
    public static int stripAt(double u) {
        if (Math.abs(u - MASTER_U) <= 1.4) {
            return 8;
        }
        for (int i = 0; i < 8; i++) {
            if (Math.abs(u - stripU(i)) <= 1.3) {
                return i;
            }
        }
        return -1;
    }
}
