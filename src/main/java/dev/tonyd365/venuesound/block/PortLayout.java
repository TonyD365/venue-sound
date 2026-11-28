package dev.tonyd365.venuesound.block;

import java.util.ArrayList;
import java.util.List;

/**
 * The sockets on every device, in panel units (see {@link PanelMath}): which face they are on,
 * where, and whether a cable plugged in there carries signal in or out. Pure constants with no
 * Minecraft types, shared with the Workbench app.
 */
public final class PortLayout {

    private PortLayout() {
    }

    public enum Face { FRONT, BACK, ANY }

    public enum Dir { IN, OUT, BOTH }

    /**
     * @param index  for mixer inputs, the input number (0-7); otherwise the socket's own number
     * @param depth  depth of the face the socket sits on (0 = front edge, 16 = back edge)
     */
    public record Port(String label, Dir dir, Face face, int index, double u0, double u1, double h0, double h1, double depth) {

        public double u() {
            return (u0 + u1) / 2;
        }

        public double h() {
            return (h0 + h1) / 2;
        }

        public boolean contains(double u, double h) {
            return u >= u0 - 0.25 && u <= u1 + 0.25 && h >= h0 - 0.25 && h <= h1 + 0.25;
        }

        public boolean acceptsIn() {
            return dir != Dir.OUT;
        }

        public boolean givesOut() {
            return dir != Dir.IN;
        }
    }

    /** Device keys match the block ids: receiver, stand_mic, media_deck, outlet, mixer, amplifier, speaker, pa_speaker. */
    public static List<Port> ports(String device) {
        List<Port> p = new ArrayList<>();
        switch (device) {
            case "receiver" -> p.add(new Port("OUT", Dir.OUT, Face.BACK, 0, 7.0, 9.0, 1.2, 3.4, 13.0));
            case "stage_box" -> {
                for (int i = 0; i < 8; i++) {
                    double u = 1.5 + (i % 4) * 3.4, h = i < 4 ? 10.0 : 5.5;
                    p.add(new Port("CH " + (i + 1), Dir.IN, Face.FRONT, i, u, u + 2.4, h, h + 2.4, 14.0));
                }
                p.add(new Port("SNAKE", Dir.OUT, Face.FRONT, 0, 6.0, 10.0, 1.0, 4.0, 14.0));
            }
            case "stage_box_16" -> {
                for (int i = 0; i < 16; i++) {
                    double u = 0.8 + (i % 8) * 1.8, h = i < 8 ? 11.0 : 7.5;
                    p.add(new Port("CH " + (i + 1), Dir.IN, Face.FRONT, i, u, u + 1.4, h, h + 1.4, 14.0));
                }
                p.add(new Port("SNAKE", Dir.OUT, Face.FRONT, 0, 6.0, 10.0, 1.0, 4.0, 14.0));
            }
            case "stand_mic", "gooseneck_mic", "choir_mic" -> p.add(new Port("OUT", Dir.OUT, Face.ANY, 0, 0, 16, 0, 16, 8.0));
            case "media_deck", "video_capture" -> p.add(new Port("OUT", Dir.OUT, Face.BACK, 0, 6.5, 9.5, 2.0, 5.0, 16.0));
            case "outlet" -> {
                // four sockets on the plate; each can take a cable in or send one on
                int n = 0;
                for (double h : new double[] {9.0, 4.5}) {
                    for (double u : new double[] {4.0, 9.0}) {
                        p.add(new Port("S" + (n + 1), Dir.BOTH, Face.FRONT, n, u, u + 3.0, h, h + 3.0, 14.0));
                        n++;
                    }
                }
            }
            case "mixer", "mixer24" -> {
                MixerLayout L = MixerLayout.of(device);
                // on the rear side facing the stage, behind each channel strip, so cables don't hang in
                // front of the operator (ST1 is the built-in USB player: no socket)
                for (int i = 0; i < L.strips; i++) {
                    if (i == L.usbStrip) {
                        continue;
                    }
                    double u = MixerLayout.stripU(i);
                    p.add(new Port(i < L.mono ? "IN " + (i + 1) : "IN " + L.stripName(i), Dir.IN, Face.BACK, i, u - 1.1, u + 1.1, 2.4, 5.6, MixerLayout.DEPTH));
                }
                String[] outs = {"MAIN L", "MAIN R", "AUX 1", "AUX 2"};
                double[] us = {45.1, 47.0, 39.3, 41.2};
                for (int b = 0; b < 4; b++) {
                    p.add(new Port(outs[b], Dir.OUT, Face.BACK, b, L.width - 48 + us[b] - 0.9, L.width - 48 + us[b] + 0.9, 2.4, 5.6, MixerLayout.DEPTH));
                }
            }
            case "amplifier", "graphic_eq", "compressor", "di_box", "fx_processor", "crossover" -> {
                p.add(new Port("IN A", Dir.IN, Face.BACK, 0, 2.0, 4.6, 9.5, 12.5, 16.0));
                p.add(new Port("IN B", Dir.IN, Face.BACK, 1, 5.2, 7.8, 9.5, 12.5, 16.0));
                for (int i = 0; i < 4; i++) {
                    double u = 6.5 + i * 2.3;
                    p.add(new Port("OUT " + (i + 1), Dir.OUT, Face.BACK, i, u, u + 1.9, 3.0, 6.0, 16.0));
                }
            }
            case "speaker", "floor_wedge", "subwoofer", "flown_array", "iem_transmitter" -> p.add(new Port("INPUT", Dir.IN, Face.BACK, 0, 6.5, 9.5, 2.5, 5.5, 16.0));
            case "pa_speaker" -> p.add(new Port("INPUT", Dir.IN, Face.BACK, 0, 6.0, 10.0, 2.0, 6.0, 15.0));
            case "concert_speaker" -> p.add(new Port("INPUT", Dir.IN, Face.BACK, 0, 6.0, 10.0, 2.0, 6.0, 32.0));
            default -> {
            }
        }
        return p;
    }

    /** The socket at panel point (u, h) on the given face, or null. */
    public static Port at(String device, Face face, double u, double h) {
        for (Port port : ports(device)) {
            if ((port.face() == face || port.face() == Face.ANY) && port.contains(u, h)) {
                return port;
            }
        }
        return null;
    }

    public static Port byIndex(String device, Dir wanted, int index) {
        for (Port port : ports(device)) {
            boolean ok = wanted == Dir.IN ? port.acceptsIn() : port.givesOut();
            if (ok && port.index() == index) {
                return port;
            }
        }
        return null;
    }

    /** Speaker rear panel: power switch and coverage knob (panel units, back face). */
    public static final class SpeakerPanel {
        public static final double SWITCH_U0 = 2.5, SWITCH_U1 = 4.5, SWITCH_H0 = 9.0, SWITCH_H1 = 12.0;
        public static final double KNOB_U = 11.0, KNOB_H = 10.5, KNOB_R = 1.8;

        private SpeakerPanel() {
        }

        public static boolean onSwitch(double u, double h) {
            return u >= SWITCH_U0 && u <= SWITCH_U1 && h >= SWITCH_H0 && h <= SWITCH_H1;
        }

        public static boolean onKnob(double u, double h) {
            return Math.hypot(u - KNOB_U, h - KNOB_H) <= KNOB_R + 0.4;
        }
    }
}
