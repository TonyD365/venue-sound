package dev.tonyd365.venuesound.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every control on the 3x2 mixing console, styled after a real 10-channel analogue console:
 * 8 mono strips, 2 stereo strips (9/10 is the built-in USB player) and a master section.
 *
 * Coordinates are panel units (see {@link PanelMath}): u 0-48 across the three blocks from the
 * operator's left, depth 0-32 from the front edge, on the flat top at height {@link #TOP}.
 * This one table drives clicks in game, the in-game renderer and the Workbench.
 */
public final class MixerLayout {

    /** VS-12: 8 mono + 2 stereo strips on 3x2 blocks. */
    public static final MixerLayout VS12 = new MixerLayout("mixer", "VS-12", 8, 2);
    /** VS-24: 16 mono + 4 stereo strips on 5x2 blocks. */
    public static final MixerLayout VS24 = new MixerLayout("mixer24", "VS-24", 16, 4);
    /** Most strips any console has (sizes input arrays). */
    public static final int MAX_STRIPS = 20;

    public static MixerLayout of(String deviceKey) {
        return "mixer24".equals(deviceKey) ? VS24 : VS12;
    }

    public final String key;
    public final String model;
    public final double width;
    public final int mono;
    public final int strips;
    /** The first stereo strip is fed by the console's own USB player. */
    public final int usbStrip;
    /** Blocks across. */
    public final int cols;

    public static final double DEPTH = 32.0;
    public static final double TOP = 8.0;

    public static final double FADER_MIN = 1.5;
    public static final double FADER_MAX = 10.5;
    public static final double KNOB_R = 0.85;

    public enum Kind { KNOB, FADER, BUTTON, LED, DISPLAY, JACK }

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

    public String stripName(int i) {
        if (i < mono) {
            return String.valueOf(i + 1);
        }
        int ch = mono + 2 * (i - mono) + 1;
        return ch + "/" + (ch + 1) + (i == usbStrip ? " USB" : "");
    }

    public static double faderDepth(double value01) {
        return FADER_MIN + value01 * (FADER_MAX - FADER_MIN);
    }

    public static double faderValue(double depth) {
        return Math.max(0, Math.min(1, (depth - FADER_MIN) / (FADER_MAX - FADER_MIN)));
    }

    private final List<Control> CONTROLS = new ArrayList<>();
    private final Map<String, Float> DEFAULTS = new LinkedHashMap<>();

    private void knob(String param, String label, double u, double depth, double min, double max, double step, double def, int color) {
        CONTROLS.add(new Control(param, Kind.KNOB, label, u, depth, KNOB_R * 2, KNOB_R * 2, min, max, step, color));
        DEFAULTS.put(param, (float) def);
    }

    private void fader(String param, String label, double u, double def, int color) {
        CONTROLS.add(new Control(param, Kind.FADER, label, u, FADER_MIN, 1.6, FADER_MAX - FADER_MIN, 0, 2, 0, color));
        DEFAULTS.put(param, (float) def);
    }

    private void button(String param, String label, double u, double depth, int color, boolean toggle) {
        CONTROLS.add(new Control(param, Kind.BUTTON, label, u, depth, 1.5, 1.0, 0, 1, 0, color));
        if (toggle) {
            DEFAULTS.put(param, 0F);
        }
    }

    private MixerLayout(String key, String model, int mono, int stereo) {
        this.key = key;
        this.model = model;
        this.mono = mono;
        this.strips = mono + stereo;
        this.usbStrip = mono;
        this.cols = mono == 8 ? 3 : 5;
        this.width = cols * 16.0;
        double m = width - 48.0; // master section sits at the right end
        DEFAULTS.put("layout.usb", (float) usbStrip);
        DEFAULTS.put("layout.strips", (float) strips);
        // TH-014 style: raised rear jack field (XLR over 6.35), then GAIN, EQ, sends, PEAK, buttons, fader
        for (int i = 0; i < strips; i++) {
            double u = stripU(i);
            if (i < mono) {
                CONTROLS.add(new Control("jack.xlr." + i, Kind.JACK, "XLR", u, 31.0, 1.6, 1.6, 0, 0, 0, 0x2B2D31));
            }
            CONTROLS.add(new Control("jack.trs." + i, Kind.JACK, "LINE", u, 29.4, 1.1, 1.1, 0, 0, 0, 0x2B2D31));
            knob("gain." + i, "GAIN", u, 27.4, 0, 2, 0.1, 1, 0xD0312B);
            knob("high." + i, "HI", u, 25.5, -12, 12, 2, 0, 0x2E86C1);
            knob("mid." + i, "MID", u, 23.7, -12, 12, 2, 0, 0x27AE60);
            knob("low." + i, "LOW", u, 21.9, -12, 12, 2, 0, 0x1F618D);
            knob("aux1." + i, "AUX1", u, 20.1, 0, 1, 0.1, 0, 0xF1C40F);
            knob("aux2." + i, "AUX2", u, 18.3, 0, 1, 0.1, 0, 0xE67E22);
            knob("efx." + i, "EFX", u, 16.5, 0, 1, 0.1, 0, 0x8E44AD);
            knob("pan." + i, "PAN", u, 14.7, -1, 1, 0.2, 0, 0xECF0F1);
            CONTROLS.add(new Control("peak." + i, Kind.LED, "PEAK", u, 13.4, 0.5, 0.5, 0, 1, 0, 0xFF3030));
            button("mute." + i, "MUTE", u, 12.5, 0xE74C3C, true);
            button("pfl." + i, "PFL", u, 11.4, 0xF39C12, true);
            fader("fader." + i, stripName(i), u, 1, 0xF2F2F2);
        }
        // master section: LCD (USB player) flanked by two knobs, button row, meter column, coloured faders
        CONTROLS.add(new Control("display", Kind.DISPLAY, "USB PLAYER", m + 42.5, 27.6, 7.0, 4.4, 0, 0, 0, 0x0B2A12));
        knob("efx.time", "DELAY", m + 37.6, 27.6, 0, 1, 0.1, 0.4, 0x8E44AD);
        knob("phones", "PHONES", m + 47.4, 27.6, 0, 1, 0.1, 0.5, 0xECF0F1);
        button("usb.prev", "|<", m + 38.6, 23.8, 0x3A3D42, false);
        button("usb.play", ">", m + 41.2, 23.8, 0x2ECC71, false);
        button("usb.stop", "[]", m + 43.8, 23.8, 0x3A3D42, false);
        button("usb.next", ">|", m + 46.4, 23.8, 0x3A3D42, false);
        for (int s = 0; s < 8; s++) {
            CONTROLS.add(new Control("meter.l." + s, Kind.LED, "", m + 42.75, 11.0 + s * 1.1, 0.5, 0.5, 0, 1, 0, s < 5 ? 0x3DDC84 : s < 7 ? 0xF1C40F : 0xE74C3C));
            CONTROLS.add(new Control("meter.r." + s, Kind.LED, "", m + 43.45, 11.0 + s * 1.1, 0.5, 0.5, 0, 1, 0, s < 5 ? 0x3DDC84 : s < 7 ? 0xF1C40F : 0xE74C3C));
        }
        fader("efx.m", "EFX", m + 37.4, 0.5, 0xF1C40F);
        fader("aux1.m", "AUX1", m + 39.3, 1, 0x2E86C1);
        fader("aux2.m", "AUX2", m + 41.2, 1, 0x2E86C1);
        fader("main.l", "L", m + 45.1, 1, 0xD0312B);
        fader("main.r", "R", m + 47.0, 1, 0xD0312B);
    }

    public List<Control> controls() {
        return Collections.unmodifiableList(CONTROLS);
    }

    /** Default value of every setting. */
    public Map<String, Float> defaults() {
        return Collections.unmodifiableMap(DEFAULTS);
    }

    /** The control under panel point (u, depth) on the top surface, or null. */
    public Control at(double u, double depth) {
        for (Control c : CONTROLS) {
            if (c.hit(u, depth)) {
                return c;
            }
        }
        return null;
    }

    public Control byParam(String param) {
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
