package dev.tonyd365.venuesound.block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Front-panel controls of the processors that have their own settings (graphic EQ, compressor),
 * in panel units on the front face. Above the amplifier-style power rocker and level knob.
 * Pure constants, shared with the Workbench.
 */
public final class ProcessorLayout {

    private ProcessorLayout() {
    }

    /** A slider (click upper half: up, lower half: down) or knob (left half: down, right half: up). */
    public record Ctl(String param, String label, boolean slider, double u, double h, double min, double max, double step) {

        public static final double SLIDER_H0 = 11.0, SLIDER_H1 = 14.6, KNOB_R = 1.2;

        public boolean hit(double pu, double ph) {
            return slider ? Math.abs(pu - u) <= 0.8 && ph >= SLIDER_H0 - 0.2 && ph <= SLIDER_H1 + 0.2
                    : Math.hypot(pu - u, ph - h) <= KNOB_R + 0.3;
        }

        public float apply(float current, double pu, double ph) {
            boolean up = slider ? ph > (SLIDER_H0 + SLIDER_H1) / 2 : pu > u;
            double next = Math.round((current + (up ? step : -step)) / step) * step;
            return (float) Math.max(min, Math.min(max, next));
        }

        public double t(float v) {
            return (v - min) / (max - min);
        }
    }

    public static final String[] EQ_BANDS = {"63", "160", "400", "1K", "2.5K", "6.3K", "16K"};

    public static List<Ctl> controls(String device) {
        List<Ctl> c = new ArrayList<>();
        if (device.equals("graphic_eq")) {
            for (int i = 0; i < EQ_BANDS.length; i++) {
                c.add(new Ctl("eq." + i, EQ_BANDS[i], true, 1.8 + i * 2.1, 12.8, -12, 12, 3));
            }
        } else if (device.equals("compressor")) {
            c.add(new Ctl("comp.thr", "THRESH", false, 2.5, 12.9, 0.1, 1, 0.1));
            c.add(new Ctl("comp.ratio", "RATIO", false, 6.5, 12.9, 1, 10, 1));
        } else if (device.equals("fx_processor")) {
            c.add(new Ctl("fx.mix", "MIX", false, 2.5, 12.9, 0, 1, 0.1));
            c.add(new Ctl("fx.time", "TIME", false, 6.5, 12.9, 0, 1, 0.1));
        } else if (device.equals("crossover")) {
            c.add(new Ctl("xo.mode", "FULL/LOW/HIGH", false, 2.5, 12.9, 0, 2, 1));
        }
        return c;
    }

    /** Default settings; the "proc.*" marker tells the signal engine which processing to run. */
    public static Map<String, Float> defaults(String device) {
        Map<String, Float> m = new LinkedHashMap<>();
        if (device.equals("graphic_eq")) {
            m.put("proc.eq", 1F);
            for (int i = 0; i < EQ_BANDS.length; i++) {
                m.put("eq." + i, 0F);
            }
        } else if (device.equals("compressor")) {
            m.put("proc.comp", 1F);
            m.put("comp.thr", 0.5F);
            m.put("comp.ratio", 4F);
        } else if (device.equals("fx_processor")) {
            m.put("proc.fx", 1F);
            m.put("fx.mix", 0.4F);
            m.put("fx.time", 0.4F);
        } else if (device.equals("crossover")) {
            m.put("proc.xo", 1F);
            m.put("xo.mode", 0F);
        }
        return m;
    }

    public static Ctl at(String device, double u, double h) {
        for (Ctl c : controls(device)) {
            if (c.hit(u, h)) {
                return c;
            }
        }
        return null;
    }
}
