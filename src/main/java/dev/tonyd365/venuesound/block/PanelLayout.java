package dev.tonyd365.venuesound.block;

/**
 * Front-panel layouts of the amplifier, media deck and receiver, in panel units (see
 * {@link PanelMath}). Pure constants with no Minecraft types, shared with the Workbench app.
 */
public final class PanelLayout {

    private PanelLayout() {
    }

    /** Power amplifier: rocker switch (power) and level knob. */
    public static final class Amp {
        public static final double SWITCH_U0 = 2.0, SWITCH_U1 = 5.0, SWITCH_H0 = 6.0, SWITCH_H1 = 10.0;
        public static final double KNOB_U = 10.5, KNOB_H = 8.0, KNOB_R = 2.6;
        public static final int[] LEVELS = {25, 50, 75, 100, 125, 150, 175, 200};

        private Amp() {
        }

        public static boolean onSwitch(double u, double h) {
            return u >= SWITCH_U0 && u <= SWITCH_U1 && h >= SWITCH_H0 && h <= SWITCH_H1;
        }

        public static boolean onKnob(double u, double h) {
            return Math.hypot(u - KNOB_U, h - KNOB_H) <= KNOB_R + 0.5;
        }
    }

    /** Media deck: display and a row of buttons. */
    public static final class Deck {
        public static final String[] BUTTONS = {"prev", "play", "stop", "next", "loop"};
        public static final double BUTTON_H0 = 1.0, BUTTON_H1 = 3.0, BUTTON_W = 2.0;
        public static final double DISPLAY_U0 = 2.0, DISPLAY_U1 = 14.0, DISPLAY_H0 = 4.2, DISPLAY_H1 = 7.2;

        private Deck() {
        }

        public static double buttonU0(int i) {
            return 2.0 + i * 2.5;
        }

        /** The button at (u, h), or null. */
        public static String buttonAt(double u, double h) {
            if (h < BUTTON_H0 || h > BUTTON_H1) {
                return null;
            }
            for (int i = 0; i < BUTTONS.length; i++) {
                if (u >= buttonU0(i) && u <= buttonU0(i) + BUTTON_W) {
                    return BUTTONS[i];
                }
            }
            return null;
        }
    }

    /** Wireless receiver: channel display and down/up buttons. */
    public static final class Receiver {
        public static final double FRONT_DEPTH = 3.0;
        public static final double DOWN_U0 = 10.0, UP_U0 = 12.3, BUTTON_W = 1.8, BUTTON_H0 = 1.5, BUTTON_H1 = 3.5;

        private Receiver() {
        }

        /** The channel display; clicking it steps to the next channel. */
        public static final double DISPLAY_U0 = 2.5, DISPLAY_U1 = 8.5, DISPLAY_H0 = 1.3, DISPLAY_H1 = 4.0;

        /** -1 for the down button, +1 for up (or the display), 0 for neither. */
        public static int buttonAt(double u, double h) {
            if (u >= DISPLAY_U0 - 0.3 && u <= DISPLAY_U1 + 0.3 && h >= DISPLAY_H0 - 0.3 && h <= DISPLAY_H1 + 0.3) {
                return 1;
            }
            if (h < BUTTON_H0 - 0.4 || h > BUTTON_H1 + 0.4) {
                return 0;
            }
            if (u >= DOWN_U0 - 0.3 && u <= DOWN_U0 + BUTTON_W + 0.15) {
                return -1;
            }
            return u > DOWN_U0 + BUTTON_W + 0.15 && u <= UP_U0 + BUTTON_W + 0.3 ? 1 : 0;
        }
    }
}
