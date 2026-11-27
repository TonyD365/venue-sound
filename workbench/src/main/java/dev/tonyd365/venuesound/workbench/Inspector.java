package dev.tonyd365.venuesound.workbench;

import dev.tonyd365.venuesound.block.MixerLayout;
import dev.tonyd365.venuesound.block.PanelLayout;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Map;

/**
 * A close-up of the selected device's control surface. Uses the same layouts as the blocks in the
 * game, so clicking and dragging here behaves like right-clicking and holding on the model.
 */
public final class Inspector extends JPanel {

    private static final Color BG = new Color(0x202226);
    private static final Color PANEL = new Color(0x2B2E33);
    private static final Color TEXT = new Color(0xE8E8E8);
    private static final Color DIM = new Color(0x9AA0A6);
    private static final Color GOLD = new Color(0xE8B84A);
    private static final Color GREEN = new Color(0x3DDC84);

    private final Scene scene;
    private final AudioEngine audio;
    private final Runnable changed;
    private Device device;

    public Inspector(Scene scene, AudioEngine audio, Runnable changed) {
        this.scene = scene;
        this.audio = audio;
        this.changed = changed;
        setBackground(BG);
        setPreferredSize(new Dimension(720, 560));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                click(e.getX(), e.getY(), true);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                click(e.getX(), e.getY(), false);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = null;
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public void show(Device d) {
        this.device = d;
        repaint();
    }

    // ------------------------------------------------------------------ geometry

    private static final double MS = 14.2;   // console: pixels per panel unit
    private static final int MX = 12, MY = 66;
    private static final double FS = 24;     // front panels: pixels per panel unit
    private static final int FX = 60, FY = 70;

    private double cx(double u) {
        return MX + u * MS;
    }

    private double cy(double depth) {
        return MY + (MixerLayout.DEPTH - depth) * MS;
    }

    private double fx(double u) {
        return FX + u * FS;
    }

    private double fy(double h, double height) {
        return FY + (height - h) * FS;
    }

    // ------------------------------------------------------------------ input

    private void click(int x, int y, boolean press) {
        if (device == null) {
            return;
        }
        switch (device.type) {
            case MIXER -> clickMixer(x, y, press);
            case AMPLIFIER, GRAPHIC_EQ, COMPRESSOR, DI_BOX, FX_PROCESSOR, CROSSOVER -> {
                if (!press) {
                    return;
                }
                double u = (x - FX) / FS, h = 16 - (y - FY) / FS;
                var pc = dev.tonyd365.venuesound.block.ProcessorLayout.at(device.type.key, u, h);
                if (pc != null) {
                    float cur = (float) scene.graph.param(device, pc.param());
                    device.params.put(pc.param(), pc.apply(cur, u, h));
                } else if (PanelLayout.Amp.onSwitch(u, h)) {
                    device.ampOn = !device.ampOn;
                } else if (PanelLayout.Amp.onKnob(u, h)) {
                    device.stepAmpLevel(u < PanelLayout.Amp.KNOB_U);
                }
            }
            case POWER_SEQUENCER -> {
                if (press) {
                    boolean target = scene.devices.stream().noneMatch(d -> d.type == Device.Type.AMPLIFIER && d.ampOn);
                    for (Device d : scene.devices) {
                        if (d.type == Device.Type.AMPLIFIER) {
                            d.ampOn = target;
                        } else if (d.type.kind == dev.tonyd365.venuesound.audio.NodeKind.SPEAKER) {
                            d.speakerOn = target;
                        }
                    }
                }
            }
            case MEDIA_DECK -> {
                if (!press) {
                    return;
                }
                double u = (x - FX) / FS, h = 8 - (y - FY) / FS;
                String button = PanelLayout.Deck.buttonAt(u, h);
                if (button != null) {
                    audio.pressDeck(device, button);
                }
            }
            case RECEIVER -> {
                if (!press) {
                    return;
                }
                double u = (x - FX) / FS, h = 5 - (y - FY) / FS;
                int b = PanelLayout.Receiver.buttonAt(u, h);
                if (b != 0) {
                    device.channel = Math.floorMod(device.channel - 1 + b, 16) + 1;
                }
            }
            case PA_SPEAKER, LINE_ARRAY, CONCERT_SPEAKER, FLOOR_WEDGE, SUBWOOFER, FLOWN_ARRAY, IEM_TRANSMITTER -> {
                if (press && y > 300 && y < 340) {
                    device.stepRange(x < 200);
                } else if (press && y > 360 && y < 400 && x >= 40 && x <= 360) {
                    device.speakerOn = !device.speakerOn;
                }
            }
            default -> {
            }
        }
        changed.run();
        repaint();
    }

    private MixerLayout.Control dragging;

    private void clickMixer(int x, int y, boolean press) {
        double u = (x - MX) / MS;
        double depth = MixerLayout.DEPTH - (y - MY) / MS;
        MixerLayout.Control c = press ? MixerLayout.at(u, depth) : dragging;
        if (press) {
            dragging = c != null && c.kind() == MixerLayout.Kind.FADER ? c : null;
        }
        if (c == null || (!press && c.kind() != MixerLayout.Kind.FADER)) {
            return;
        }
        if (c.kind() == MixerLayout.Kind.BUTTON && c.param().startsWith("usb.")) {
            audio.pressUsb(device, c.param().substring(4));
            return;
        }
        device.params.put(c.param(), MixerLayout.apply(c, device.get(c.param()), u, depth));
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (device == null) {
            g.setColor(DIM);
            g.setFont(getFont().deriveFont(15f));
            g.drawString("Click a device on the floor to use its controls.", 20, 40);
            g.drawString("Toolbar: place devices, run cables, talk into your mic.", 20, 66);
            return;
        }
        g.setColor(GOLD);
        g.setFont(getFont().deriveFont(Font.BOLD, 20f));
        g.drawString(device.type.label, 16, 32);
        g.setFont(getFont().deriveFont(12f));
        g.setColor(DIM);
        g.drawString(hint(), 16, 52);
        switch (device.type) {
            case MIXER -> drawMixer(g);
            case AMPLIFIER, GRAPHIC_EQ, COMPRESSOR, DI_BOX, FX_PROCESSOR, CROSSOVER -> drawAmp(g);
            case MEDIA_DECK -> drawDeck(g);
            case RECEIVER -> drawReceiver(g);
            case PA_SPEAKER, LINE_ARRAY, CONCERT_SPEAKER, FLOOR_WEDGE, SUBWOOFER, FLOWN_ARRAY, IEM_TRANSMITTER -> drawSpeaker(g);
            default -> {
            }
        }
        drawRouting(g);
    }

    private String hint() {
        return switch (device.type) {
            case MIXER -> "Drag faders · click buttons · left/right half of a knob turns it down/up · USB player in the master section";
            case AMPLIFIER, GRAPHIC_EQ, COMPRESSOR, DI_BOX, FX_PROCESSOR, CROSSOVER -> "Click the rocker for power · left/right half of the knob for level";
            case MEDIA_DECK -> "Buttons: previous, play, stop, next, loop · files from " + scene.mediaFolder.getFileName();
            case RECEIVER -> "Picks up the wireless mic on this channel (set yours in the toolbar)";
            case STAND_MIC -> "Picks up your voice when you stand within 3 blocks";
            case OUTLET -> "Patch point: chain cables or split one signal into up to 8";
            case POWER_SEQUENCER -> "Click to switch every amplifier and speaker on (speakers first) or off (amps first)";
            case RACK_CABINET, TRUSS_POINT -> "Mounting hardware: no signal";
            case CABLE_CLIP -> "Holds cables to the floor or a wall";
            default -> "Rear panel: INPUT socket, power switch and coverage knob";
        };
    }

    private void drawMixer(Graphics2D g) {
        g.setColor(new Color(0x23262B));
        g.fill(new RoundRectangle2D.Double(cx(0), cy(MixerLayout.DEPTH), MixerLayout.WIDTH * MS, MixerLayout.DEPTH * MS, 14, 14));
        for (int i = 0; i < MixerLayout.STRIPS; i++) {
            double u = MixerLayout.stripU(i);
            g.setColor(i % 2 == 0 ? PANEL : new Color(0x272A2F));
            g.fill(new RoundRectangle2D.Double(cx(u - 1.7), cy(31.7), 3.4 * MS, 31.4 * MS, 6, 6));
            g.setColor(device.inputs[i] != null || (i == MixerLayout.USB_STRIP && device.usbPlaying) ? GREEN : DIM);
            g.setFont(getFont().deriveFont(Font.BOLD, 10f));
            String src = i == MixerLayout.USB_STRIP ? "USB" : Scene.sourceLabel(device.inputs[i]);
            g.drawString(MixerLayout.stripName(i).replace(" USB", ""), (float) cx(u - 1.5), (float) cy(31.85) - 1);
            g.drawString(src, (float) cx(u - 1.5), (float) cy(0.05) + 10);
        }
        g.setColor(new Color(0x2E261C));
        g.fill(new RoundRectangle2D.Double(cx(36.6), cy(31.7), 11.2 * MS, 31.4 * MS, 6, 6));
        boolean active = device.usbPlaying;
        for (Device d : device.inputs) {
            active |= d != null;
        }
        for (MixerLayout.Control c : MixerLayout.controls()) {
            double u = c.u(), dd = c.depth();
            switch (c.kind()) {
                case KNOB -> {
                    float v = device.get(c.param());
                    double r = MixerLayout.KNOB_R;
                    g.setColor(new Color(0x141518));
                    g.fill(new Ellipse2D.Double(cx(u - r), cy(dd + r), 2 * r * MS, 2 * r * MS));
                    g.setColor(new Color(c.color()));
                    g.fill(new Ellipse2D.Double(cx(u - r * 0.68), cy(dd + r * 0.68), 1.36 * r * MS, 1.36 * r * MS));
                    double a = MixerLayout.knobAngle(c, v);
                    g.setColor(Color.WHITE);
                    g.setStroke(new BasicStroke(2f));
                    g.draw(new Line2D.Double(cx(u), cy(dd), cx(u + Math.sin(a) * r * 0.8), cy(dd + Math.cos(a) * r * 0.8)));
                    g.setStroke(new BasicStroke(1f));
                    g.setColor(DIM);
                    g.setFont(getFont().deriveFont(8f));
                    if (u > 36) {
                        g.drawString(c.label(), (float) cx(u - 1.0), (float) cy(dd - 1.0) + 8);
                    } else {
                        g.drawString(c.label(), (float) cx(u + 0.92), (float) cy(dd) + 3);
                    }
                }
                case FADER -> {
                    float v = device.get(c.param());
                    g.setColor(Color.BLACK);
                    g.fill(new RoundRectangle2D.Double(cx(u - 0.14), cy(MixerLayout.FADER_MAX + 0.4), 0.28 * MS, (MixerLayout.FADER_MAX - MixerLayout.FADER_MIN + 0.8) * MS, 3, 3));
                    for (int k = 0; k <= 8; k++) {
                        double t = MixerLayout.faderDepth(k / 8.0);
                        g.setColor(k == 4 ? GOLD : new Color(0x6B7178));
                        g.draw(new Line2D.Double(cx(u - 0.8), cy(t), cx(u - 0.4), cy(t)));
                    }
                    double fd = MixerLayout.faderDepth(v / 2.0);
                    g.setColor(new Color(c.color()));
                    g.fill(new RoundRectangle2D.Double(cx(u - 0.62), cy(fd + 0.45), 1.24 * MS, 0.9 * MS, 4, 4));
                    g.setColor(Color.DARK_GRAY);
                    g.draw(new Line2D.Double(cx(u - 0.62), cy(fd), cx(u + 0.62), cy(fd)));
                    if (c.param().contains(".m") || c.param().startsWith("main")) {
                        g.setColor(TEXT);
                        g.setFont(getFont().deriveFont(Font.BOLD, 9f));
                        g.drawString(c.label(), (float) cx(u - 0.6), (float) cy(0.6) + 2);
                    }
                }
                case BUTTON -> {
                    boolean on = c.param().startsWith("usb.") ? c.param().equals("usb.play") && device.usbPlaying : device.get(c.param()) > 0.5F;
                    g.setColor(on ? new Color(c.color()) : new Color(0x3A3D42));
                    g.fill(new RoundRectangle2D.Double(cx(u - c.w() / 2), cy(dd + c.d() / 2), c.w() * MS, c.d() * MS, 4, 4));
                    g.setColor(on ? Color.BLACK : TEXT);
                    g.setFont(getFont().deriveFont(Font.BOLD, 8.5f));
                    g.drawString(c.label(), (float) cx(u - c.w() / 2 + 0.15), (float) cy(dd - 0.25));
                }
                case LED -> {
                    boolean on;
                    if (c.param().startsWith("peak.")) {
                        int st = Integer.parseInt(c.param().substring(5));
                        on = device.get("gain." + st) * device.get("fader." + st) > 1.6F && device.get("mute." + st) < 0.5F;
                    } else {
                        int step = Integer.parseInt(c.param().substring(c.param().lastIndexOf('.') + 1));
                        float lvl = c.param().startsWith("meter.l") ? device.get("main.l") : device.get("main.r");
                        on = active && step < Math.round(audio.meter(device) * 8 + lvl);
                    }
                    g.setColor(on ? new Color(c.color()) : new Color(0x2A2C30));
                    g.fill(new Ellipse2D.Double(cx(u - 0.25), cy(dd + 0.25), 0.5 * MS, 0.5 * MS));
                }
                case DISPLAY -> {
                    g.setColor(new Color(0x0B2A12));
                    g.fill(new RoundRectangle2D.Double(cx(u - c.w() / 2), cy(dd + c.d() / 2), c.w() * MS, c.d() * MS, 6, 6));
                    g.setColor(GREEN);
                    g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
                    g.drawString("USB PLAYER " + (device.usbPlaying ? "▶" : "■"), (float) cx(u - c.w() / 2 + 0.4), (float) cy(dd + 1.2));
                    String t = device.usbTrack.isEmpty() ? "NO TRACK" : device.usbTrack;
                    g.drawString(t.length() > 20 ? t.substring(0, 19) + "~" : t, (float) cx(u - c.w() / 2 + 0.4), (float) cy(dd - 0.6));
                }
            }
        }
    }

    private void frontBody(Graphics2D g, double height) {
        g.setColor(new Color(0x1F2124));
        g.fill(new RoundRectangle2D.Double(fx(0), fy(height, height), 16 * FS, height * FS, 10, 10));
    }

    private void drawAmp(Graphics2D g) {
        frontBody(g, 16);
        boolean on = device.ampOn;
        g.setColor(new Color(0x15161A));
        g.fill(new RoundRectangle2D.Double(fx(PanelLayout.Amp.SWITCH_U0), fy(PanelLayout.Amp.SWITCH_H1, 16),
                (PanelLayout.Amp.SWITCH_U1 - PanelLayout.Amp.SWITCH_U0) * FS, (PanelLayout.Amp.SWITCH_H1 - PanelLayout.Amp.SWITCH_H0) * FS, 6, 6));
        double mid = (PanelLayout.Amp.SWITCH_H0 + PanelLayout.Amp.SWITCH_H1) / 2;
        g.setColor(on ? new Color(0xE74C3C) : new Color(0x3A3D42));
        g.fill(new RoundRectangle2D.Double(fx(PanelLayout.Amp.SWITCH_U0 + 0.3), fy(on ? PanelLayout.Amp.SWITCH_H1 - 0.3 : mid, 16),
                (PanelLayout.Amp.SWITCH_U1 - PanelLayout.Amp.SWITCH_U0 - 0.6) * FS, (mid - PanelLayout.Amp.SWITCH_H0 - 0.3) * FS, 5, 5));
        g.setColor(TEXT);
        g.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g.drawString(on ? "POWER ON" : "POWER OFF", (float) fx(1.6), (float) fy(PanelLayout.Amp.SWITCH_H0 - 1, 16));
        double r = PanelLayout.Amp.KNOB_R, ku = PanelLayout.Amp.KNOB_U, kh = PanelLayout.Amp.KNOB_H;
        g.setColor(new Color(0x1A1B1E));
        g.fill(new Ellipse2D.Double(fx(ku - r), fy(kh + r, 16), 2 * r * FS, 2 * r * FS));
        g.setColor(new Color(0x9AA0A6));
        g.fill(new Ellipse2D.Double(fx(ku - r + 0.6), fy(kh + r - 0.6, 16), 2 * (r - 0.6) * FS, 2 * (r - 0.6) * FS));
        for (var pc : dev.tonyd365.venuesound.block.ProcessorLayout.controls(device.type.key)) {
            double t = pc.t((float) scene.graph.param(device, pc.param()));
            g.setColor(new Color(0x1A1B1E));
            if (pc.slider()) {
                g.fillRect((int) fx(pc.u() - 0.2), (int) fy(pc.SLIDER_H1, 16), (int) (0.4 * FS), (int) ((pc.SLIDER_H1 - pc.SLIDER_H0) * FS));
                g.setColor(new Color(0xE8E8E8));
                double hh = pc.SLIDER_H0 + t * (pc.SLIDER_H1 - pc.SLIDER_H0);
                g.fillRect((int) fx(pc.u() - 0.6), (int) fy(hh + 0.3, 16), (int) (1.2 * FS), (int) (0.6 * FS));
            } else {
                double kr = pc.KNOB_R;
                g.fill(new Ellipse2D.Double(fx(pc.u() - kr), fy(pc.h() + kr, 16), 2 * kr * FS, 2 * kr * FS));
                g.setColor(new Color(0x9AA0A6));
                double ka = Math.toRadians(-135 + t * 270);
                g.fill(new Ellipse2D.Double(fx(pc.u() + Math.sin(ka) * kr * 0.6 - 0.25), fy(pc.h() + Math.cos(ka) * kr * 0.6 + 0.25, 16), 0.5 * FS, 0.5 * FS));
            }
            g.setColor(TEXT);
            g.setFont(getFont().deriveFont(10f));
            g.drawString(pc.label(), (float) fx(pc.u() - 0.8), (float) fy(pc.slider() ? pc.SLIDER_H0 - 0.6 : pc.h() - pc.KNOB_R - 0.6, 16));
        }
        double a = Math.toRadians(-135 + (device.ampLevel - 25) / 175.0 * 270);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(4f));
        g.draw(new Line2D.Double(fx(ku), fy(kh, 16), fx(ku + Math.sin(a) * (r - 0.8)), fy(kh + Math.cos(a) * (r - 0.8), 16)));
        g.setStroke(new BasicStroke(1f));
        g.setColor(GOLD);
        g.drawString("LEVEL " + device.ampLevel + "%", (float) fx(ku - 2.2), (float) fy(kh - r - 1.2, 16));
        g.setColor(on ? GREEN : new Color(0x444444));
        g.fill(new Ellipse2D.Double(fx(12.6), fy(13.8, 16), 0.8 * FS, 0.8 * FS));
    }

    private void drawDeck(Graphics2D g) {
        frontBody(g, 8);
        g.setColor(device.playing ? new Color(0x0F3A1C) : new Color(0x0B2A12));
        g.fillRect((int) fx(PanelLayout.Deck.DISPLAY_U0), (int) fy(PanelLayout.Deck.DISPLAY_H1, 8),
                (int) ((PanelLayout.Deck.DISPLAY_U1 - PanelLayout.Deck.DISPLAY_U0) * FS), (int) ((PanelLayout.Deck.DISPLAY_H1 - PanelLayout.Deck.DISPLAY_H0) * FS));
        g.setColor(GREEN);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 15));
        String name = device.track.isEmpty() ? "NO TRACK" : device.track;
        g.drawString((device.playing ? "▶ " : "  ") + name, (float) fx(2.3), (float) fy(6.0, 8));
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        g.drawString((device.loop ? "LOOP  " : "") + audio.deckStatus(device), (float) fx(2.3), (float) fy(4.6, 8));
        String[] labels = {"|◀", "▶", "■", "▶|", "LOOP"};
        for (int i = 0; i < labels.length; i++) {
            double u = PanelLayout.Deck.buttonU0(i);
            boolean lit = (i == 1 && device.playing) || (i == 4 && device.loop);
            g.setColor(lit ? new Color(0x2ECC71) : new Color(0x3A3D42));
            g.fill(new RoundRectangle2D.Double(fx(u), fy(PanelLayout.Deck.BUTTON_H1, 8), PanelLayout.Deck.BUTTON_W * FS,
                    (PanelLayout.Deck.BUTTON_H1 - PanelLayout.Deck.BUTTON_H0) * FS, 6, 6));
            g.setColor(TEXT);
            g.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g.drawString(labels[i], (float) fx(u + 0.35), (float) fy(1.7, 8));
        }
    }

    private void drawReceiver(Graphics2D g) {
        frontBody(g, 5);
        g.setColor(new Color(0x0B2A12));
        g.fillRect((int) fx(2.5), (int) fy(4.0, 5), (int) (6 * FS), (int) (2.7 * FS));
        g.setColor(GREEN);
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        g.drawString("CH " + device.channel, (float) fx(2.9), (float) fy(1.8, 5));
        for (double u0 : new double[] {PanelLayout.Receiver.DOWN_U0, PanelLayout.Receiver.UP_U0}) {
            g.setColor(new Color(0x9AA0A6));
            g.fill(new RoundRectangle2D.Double(fx(u0), fy(PanelLayout.Receiver.BUTTON_H1, 5), PanelLayout.Receiver.BUTTON_W * FS,
                    (PanelLayout.Receiver.BUTTON_H1 - PanelLayout.Receiver.BUTTON_H0) * FS, 6, 6));
            g.setColor(Color.BLACK);
            g.setFont(getFont().deriveFont(Font.BOLD, 22f));
            g.drawString(u0 == PanelLayout.Receiver.DOWN_U0 ? "–" : "+", (float) fx(u0 + 0.55), (float) fy(1.9, 5));
        }
    }

    private void drawSpeaker(Graphics2D g) {
        g.setColor(TEXT);
        g.setFont(getFont().deriveFont(Font.BOLD, 26f));
        g.drawString("Coverage: " + device.range + " blocks", 40, 200);
        g.setFont(getFont().deriveFont(Font.BOLD, 20f));
        g.setColor(new Color(0x3A3D42));
        g.fillRoundRect(40, 300, 140, 40, 10, 10);
        g.fillRoundRect(220, 300, 140, 40, 10, 10);
        g.setColor(TEXT);
        g.drawString("– smaller", 62, 327);
        g.drawString("+ larger", 248, 327);
        g.setColor(device.speakerOn ? new Color(0xE74C3C) : new Color(0x3A3D42));
        g.fillRoundRect(40, 360, 320, 40, 10, 10);
        g.setColor(TEXT);
        g.drawString(device.speakerOn ? "POWER ON (rear switch)" : "POWER OFF (rear switch)", 70, 387);
        double d = Math.hypot(device.x + 0.5 - scene.listenerX, device.z + 0.5 - scene.listenerZ);
        g.setFont(getFont().deriveFont(14f));
        g.setColor(DIM);
        g.drawString(String.format("You are %.1f blocks away: %s", d, d <= device.range ? "you hear it" : "out of range"), 40, 240);
    }

    private void drawRouting(Graphics2D g) {
        g.setFont(getFont().deriveFont(13f));
        int y = getHeight() - 70;
        if (device.kind() == null) {
            g.setColor(DIM);
            g.drawString("Cable clip: in Cable mode, click clips after the OUT socket to run the cable along them.", 16, y);
            return;
        }
        g.setColor(DIM);
        if (device.kind().isSource()) {
            Map<Device, Float> routes = scene.routes(device);
            g.drawString(routes.isEmpty() ? "Not reaching any speaker yet — cable it to a console, then to an amplifier and speakers."
                    : "Reaches " + routes.size() + " speaker(s) at " + Math.round(routes.values().iterator().next() * 100) + "% gain", 16, y);
        }
        g.drawString("Cables out: " + device.links.size() + "/" + device.kind().maxOutputs(), 16, y + 20);
        g.drawString("Level meter: " + (int) Math.round(audio.meter(device) * 100) + "%", 16, y + 40);
        g.setColor(GREEN);
        g.fillRect(150, y + 30, (int) (300 * Math.min(1, audio.meter(device))), 10);
    }
}
