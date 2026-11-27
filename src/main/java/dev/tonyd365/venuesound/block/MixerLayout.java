package dev.tonyd365.venuesound.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every control on the 3x2 mixing console, styled after a real 10-channel analogue console:
 * 8 mono strips, 2 stereo strips (ST1 is the built-in USB player) and a master section.
 *
 * Coordinates are panel units (see {@link PanelMath}): u 0-48 across the three blocks from the
 * operator's left, depth 0-32 from the front edge, on the flat top at height {@link #TOP}.
 * This one table drives clicks in game, the in-game renderer and the Workbench.
 */
public final class MixerLayout {

    private MixerLayout() {
    }

    public static final double WIDTH = 48.0;
    public static final double DEPTH = 32.0;
    public static final double TOP = 8.0;
    public static final int MONO = 8;
    public static final int STRIPS = 10;
    /** Strip 8 (ST1) is fed by the console's own USB player. */
    public static final int USB_STRIP = 8;

    public static final double FADER_MIN = 1.5;
    public static final double FADER_MAX = 10.5;
    public static final double KNOB_R = 0.85;

    public enum Kind { KNOB, FADER, BUTTON, LED, DISPLAY }

    /**
     * @param param  the setting this control changes (a key into {@link #defaults()}), or an action id for buttons
     * @param min    lowest value (knobs, faders)
     * @param max    highest value
     * @param step   change per click on a knob half
     * @param color  cap colour (0xRRGGBB)
     */
    public record Control(String param, Kind kind, String label, double u, double depth, double w, double d,
                          double min, double max, double step, int color) {

        public boolean hit(double pu, double pd) {
            return switch (kind) {
                case KNOB -> Math.hypot(pu - u, pd - depth) <= KNOB_R + 0.3;
                case FADER -> Math.abs(pu - u) <= 1.0 && pd >= FADER_MIN - 0.7 && pd <= FADER_MAX + 0.7;
                case BUTTON -> Math.abs(pu - u) <= w / 2 + 0.15 && Math.abs(pd - depth) <= d / 2 + 0.15;
                default -> false;
            };
        }
    }

    public static double stripU(int i) {
        return 2.2 + 3.5 * i;
    }

    public static String stripName(int i) {
        return i < MONO ? String.valueOf(i + 1) : i == USB_STRIP ? "ST1 USB" : "ST2";
    }

    public static double faderDepth(double value01) {
        return FADER_MIN + value01 * (FADER_MAX - FADER_MIN);
    }

    public static double faderValue(double depth) {
        return Math.max(0, Math.min(1, (depth - FADER_MIN) / (FADER_MAX - FADER_MIN)));
    }

    private static final List<Control> CONTROLS = new ArrayList<>();
    private static final Map<String, Float> DEFAULTS = new LinkedHashMap<>();

    private static void knob(String param, String label, double u, double depth, double min, double max, double step, double def, int color) {
        CONTROLS.add(new Control(param, Kind.KNOB, label, u, depth, KNOB_R * 2, KNOB_R * 2, min, max, step, color));
        DEFAULTS.put(param, (float) def);
    }

    private static void fader(String param, String label, double u, double def, int color) {
        CONTROLS.add(new Control(param, Kind.FADER, label, u, FADER_MIN, 1.6, FADER_MAX - FADER_MIN, 0, 2, 0, color));
        DEFAULTS.put(param, (float) def);
    }

    private static void button(String param, String label, double u, double depth, int color, boolean toggle) {
        CONTROLS.add(new Control(param, Kind.BUTTON, label, u, depth, 1.5, 1.0, 0, 1, 0, color));
        if (toggle) {
            DEFAULTS.put(param, 0F);
        }
    }

    static {
        for (int i = 0; i < STRIPS; i++) {
            double u = stripU(i);
            knob("gain." + i, "GAIN", u, 30.3, 0, 2, 0.1, 1, 0xC0392B);
            CONTROLS.add(new Control("peak." + i, Kind.LED, "PEAK", u + 1.35, 31.3, 0.5, 0.5, 0, 1, 0, 0xFF3030));
            knob("high." + i, "HIGH", u, 28.0, -12, 12, 2, 0, 0x2E86C1);
            knob("mid." + i, "MID", u, 25.8, -12, 12, 2, 0, 0x16A085);
            knob("low." + i, "LOW", u, 23.6, -12, 12, 2, 0, 0x1F618D);
            knob("aux1." + i, "AUX1", u, 21.4, 0, 1, 0.1, 0, 0xF1C40F);
            knob("aux2." + i, "AUX2", u, 19.2, 0, 1, 0.1, 0, 0xE67E22);
            knob("efx." + i, "EFX", u, 17.0, 0, 1, 0.1, 0, 0xECF0F1);
            knob("pan." + i, "PAN", u, 14.8, -1, 1, 0.2, 0, 0x95A5A6);
            button("mute." + i, "MUTE", u, 12.8, 0xE74C3C, true);
            button("pfl." + i, "PFL", u, 11.4, 0xF39C12, true);
            fader("fader." + i, stripName(i), u, 1, 0xD9DCE0);
        }
        // master section
        CONTROLS.add(new Control("display", Kind.DISPLAY, "USB PLAYER", 42.5, 27.8, 10.0, 5.0, 0, 0, 0, 0x0B2A12));
        button("usb.prev", "|<", 38.6, 23.6, 0x3A3D42, false);
        button("usb.play", ">", 41.2, 23.6, 0x2ECC71, false);
        button("usb.stop", "[]", 43.8, 23.6, 0x3A3D42, false);
        button("usb.next", ">|", 46.4, 23.6, 0x3A3D42, false);
        knob("efx.m", "EFX RTN", 39.0, 20.6, 0, 1, 0.1, 0.5, 0xECF0F1);
        knob("efx.time", "DELAY", 41.6, 20.6, 0, 1, 0.1, 0.4, 0xECF0F1);
        knob("phones", "PHONES", 46.4, 20.6, 0, 1, 0.1, 0.5, 0x95A5A6);
        for (int s = 0; s < 8; s++) {
            CONTROLS.add(new Control("meter.l." + s, Kind.LED, "", 42.75, 11.0 + s * 0.9, 0.5, 0.5, 0, 1, 0, s < 5 ? 0x3DDC84 : s < 7 ? 0xF1C40F : 0xE74C3C));
            CONTROLS.add(new Control("meter.r." + s, Kind.LED, "", 43.45, 11.0 + s * 0.9, 0.5, 0.5, 0, 1, 0, s < 5 ? 0x3DDC84 : s < 7 ? 0xF1C40F : 0xE74C3C));
        }
        fader("aux1.m", "AUX1", 38.4, 1, 0x2E86C1);
        fader("aux2.m", "AUX2", 40.7, 1, 0x2E86C1);
        fader("main.l", "L", 45.1, 1, 0xD0312B);
        fader("main.r", "R", 47.0, 1, 0xD0312B);
    }

    public static List<Control> controls() {
        return Collections.unmodifiableList(CONTROLS);
    }

    /** Default value of every setting. */
    public static Map<String, Float> defaults() {
        return Collections.unmodifiableMap(DEFAULTS);
    }

    /** The control under panel point (u, depth) on the top surface, or null. */
    public static Control at(double u, double depth) {
        for (Control c : CONTROLS) {
            if (c.hit(u, depth)) {
                return c;
            }
        }
        return null;
    }

    public static Control byParam(String param) {
        for (Control c : CONTROLS) {
            if (c.param().equals(param)) {
                return c;
            }
        }
        return null;
    }

    /**
     * What a click does to a setting: faders follow the click; knobs turn one step down on their
     * left half and up on their right half. Returns the new value.
     */
    public static float apply(Control c, float current, double u, double depth) {
        return switch (c.kind()) {
            case FADER -> {
                double v = faderValue(depth);
                if (Math.abs(v - 0.5) < 0.03) {
                    v = 0.5; // the 0 dB mark
                }
                yield (float) (v * 2.0);
            }
            case KNOB -> {
                double next = current + (u < c.u() ? -c.step() : c.step());
                next = Math.round(next / c.step()) * c.step();
                yield (float) Math.max(c.min(), Math.min(c.max(), next));
            }
            case BUTTON -> current > 0.5F ? 0F : 1F;
            default -> current;
        };
    }

    /** Knob pointer angle in radians: -135 deg at the minimum, +135 deg at the maximum. */
    public static double knobAngle(Control c, float value) {
        double t = (value - c.min()) / (c.max() - c.min());
        return Math.toRadians(-135 + t * 270);
    }

    /** Output buses, by MAIN/AUX OUT socket index. */
    public static final int BUS_MAIN_L = 0, BUS_MAIN_R = 1, BUS_AUX1 = 2, BUS_AUX2 = 3;
}
