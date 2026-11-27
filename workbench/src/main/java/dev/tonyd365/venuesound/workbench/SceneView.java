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
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/** The floor of the Workbench: draws the devices, their live controls and the cables, and handles the mouse. */
public final class SceneView extends JPanel {

    public enum Mode { SELECT, CABLE, PLACE }

    private final Scene scene;
    private final Models models;
    private final Models.View view = new Models.View(56);
    private Mode mode = Mode.SELECT;
    private Device.Type placeType = Device.Type.PA_SPEAKER;
    private Device.Facing placeFacing = Device.Facing.SOUTH;
    private Device selected;
    private Device cableFrom;
    private boolean draggingListener;
    private int hoverX = -1, hoverZ = -1;
    private final Consumer<Device> onSelect;
    private final Consumer<String> status;

    public SceneView(Scene scene, Models models, Consumer<Device> onSelect, Consumer<String> status) {
        this.scene = scene;
        this.models = models;
        this.onSelect = onSelect;
        this.status = status;
        setBackground(new Color(0x1B1D21));
        setPreferredSize(new Dimension((int) ((Scene.WIDTH + Scene.DEPTH * 0.38 + 1) * view.px), (int) ((Scene.DEPTH * 0.5 + 4.2) * view.px)));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                press(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingListener) {
                    double[] f = floorAt(e.getX(), e.getY());
                    scene.listenerX = Math.max(0.3, Math.min(Scene.WIDTH - 0.3, f[0]));
                    scene.listenerZ = Math.max(0.3, Math.min(Scene.DEPTH - 0.3, f[1]));
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingListener = false;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                double[] f = floorAt(e.getX(), e.getY());
                hoverX = (int) Math.floor(f[0]);
                hoverZ = (int) Math.floor(f[1]);
                if (mode == Mode.PLACE) {
                    repaint();
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private double ox() {
        return 0.5 * view.px + Scene.DEPTH * 0.38 * view.px;
    }

    private double oy() {
        return (Scene.DEPTH * 0.5 + 2.6) * view.px;
    }

    // ------------------------------------------------------------------ modes

    public void setMode(Mode mode) {
        this.mode = mode;
        cableFrom = null;
        status.accept(switch (mode) {
            case SELECT -> "Select: click a device to use its controls · drag the blue marker (you) around";
            case CABLE -> "Cable: click where the signal comes FROM, then where it goes TO · right-click a device to unplug it";
            case PLACE -> "Place: click the floor · R rotates";
        });
        repaint();
    }

    public void setPlaceType(Device.Type type) {
        this.placeType = type;
        setMode(Mode.PLACE);
    }

    public void rotate() {
        if (mode == Mode.PLACE) {
            placeFacing = placeFacing.clockwise();
        } else if (selected != null) {
            Device.Facing old = selected.facing;
            selected.facing = old.clockwise();
            scene.devices.remove(selected);
            if (!scene.isFree(selected)) {
                selected.facing = old;
            }
            scene.devices.add(selected);
        }
        repaint();
    }

    public void deleteSelected() {
        if (selected != null) {
            scene.remove(selected);
            select(null);
        }
    }

    public Device selected() {
        return selected;
    }

    private void select(Device d) {
        selected = d;
        onSelect.accept(d);
        repaint();
    }

    // ------------------------------------------------------------------ mouse

    /** Floor point (x, z) under the mouse, at height 0. */
    private double[] floorAt(int mx, int my) {
        double zDepth = (my - oy()) / (Models.View.KY * view.px); // = DEPTH - z
        double z = Scene.DEPTH - zDepth;
        double x = (mx - ox()) / view.px - zDepth * Models.View.KX;
        return new double[] {x, z};
    }

    private void press(MouseEvent e) {
        double lx = ox() + view.sx(scene.listenerX, 0, scene.listenerZ);
        double ly = oy() + view.sy(scene.listenerX, 0, scene.listenerZ);
        if (mode == Mode.SELECT && Math.hypot(e.getX() - lx, e.getY() - (ly - 14)) < 16) {
            draggingListener = true;
            return;
        }
        Device hit = pick(e.getX(), e.getY());
        switch (mode) {
            case SELECT -> select(hit);
            case CABLE -> {
                if (hit == null) {
                    cableFrom = null;
                    status.accept("Cable dropped.");
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    scene.unplug(hit);
                    status.accept("Unplugged " + hit.type.label + ".");
                } else if (cableFrom == null) {
                    if (hit.kind().maxOutputs() == 0) {
                        status.accept("Speakers are the end of the chain. Start from a source.");
                    } else {
                        cableFrom = hit;
                        status.accept("From " + hit.type.label + " — now click where the signal goes.");
                    }
                } else {
                    String error = scene.connect(cableFrom, hit);
                    status.accept(error == null ? "Connected " + cableFrom.type.label + " → " + hit.type.label + "." : error);
                    cableFrom = null;
                }
                onSelect.accept(selected);
            }
            case PLACE -> {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    setMode(Mode.SELECT);
                    return;
                }
                double[] f = floorAt(e.getX(), e.getY());
                Device d = new Device(placeType, (int) Math.floor(f[0]), (int) Math.floor(f[1]), placeFacing);
                if (scene.isFree(d)) {
                    scene.devices.add(d);
                    select(d);
                    status.accept("Placed " + d.type.label + ". Right-click to stop placing.");
                } else {
                    status.accept("No room there.");
                }
            }
        }
        repaint();
    }

    private Device pick(int mx, int my) {
        List<Device> order = sorted();
        for (int i = order.size() - 1; i >= 0; i--) {
            Device d = order.get(i);
            for (int[] c : d.cells()) {
                if (silhouette(c[0], c[1], c[2]).contains(mx, my)) {
                    return d;
                }
            }
        }
        return null;
    }

    private Path2D silhouette(double x, double y, double z) {
        Path2D p = new Path2D.Double();
        double[][] pts = {{x, y, z + 1}, {x + 1, y, z + 1}, {x + 1, y + 1, z + 1}, {x + 1, y + 1, z}, {x, y + 1, z}, {x, y, z}};
        for (int i = 0; i < pts.length; i++) {
            double sx = ox() + view.sx(pts[i][0], pts[i][1], pts[i][2]);
            double sy = oy() + view.sy(pts[i][0], pts[i][1], pts[i][2]);
            if (i == 0) {
                p.moveTo(sx, sy);
            } else {
                p.lineTo(sx, sy);
            }
        }
        p.closePath();
        return p;
    }

    private List<Device> sorted() {
        List<Device> list = new ArrayList<>(scene.devices);
        list.sort(Comparator.comparingDouble(d -> d.z - d.x * 0.01));
        return list;
    }

    // ------------------------------------------------------------------ drawing

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        double ox = ox(), oy = oy();
        drawFloor(g, ox, oy);
        List<Device> list = sorted();
        for (Device d : list) {
            for (int[] c : d.cells()) {
                String model = d.type.model;
                if (d.type == Device.Type.MIXER && c != d.cells().get(0)) {
                    model = "mixer_right";
                } else if (d.type == Device.Type.PA_SPEAKER && c[1] == 1) {
                    model = "pa_speaker_upper";
                }
                models.draw(g, view, ox, oy, model, d.facing.rotation, c[0], c[1], c[2]);
            }
            drawLiveParts(g, d);
            if (d == selected || d == cableFrom) {
                g.setColor(d == cableFrom ? new Color(0xE8B84A) : new Color(0x5DADE2));
                g.setStroke(new BasicStroke(2f));
                for (int[] c : d.cells()) {
                    g.draw(silhouette(c[0], c[1], c[2]));
                }
            }
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Device d : list) {
            for (Device out : d.outputs) {
                drawCable(g, d, out);
            }
        }
        drawListener(g);
        if (mode == Mode.PLACE && hoverX >= 0) {
            Device ghost = new Device(placeType, hoverX, hoverZ, placeFacing);
            g.setColor(scene.isFree(ghost) ? new Color(93, 173, 226, 120) : new Color(231, 76, 60, 120));
            for (int[] c : ghost.cells()) {
                g.fill(silhouette(c[0], c[1], c[2]));
            }
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    private void drawFloor(Graphics2D g, double ox, double oy) {
        for (int x = 0; x < Scene.WIDTH; x++) {
            for (int z = 0; z < Scene.DEPTH; z++) {
                Polygon p = new Polygon();
                for (double[] c : new double[][] {{x, z}, {x + 1, z}, {x + 1, z + 1}, {x, z + 1}}) {
                    p.addPoint((int) (ox + view.sx(c[0], 0, c[1])), (int) (oy + view.sy(c[0], 0, c[1])));
                }
                g.setColor(z < 7 ? ((x + z) % 2 == 0 ? new Color(0x3A2A1E) : new Color(0x35271C)) : ((x + z) % 2 == 0 ? new Color(0x2B2E33) : new Color(0x272A2F)));
                g.fillPolygon(p);
            }
        }
        g.setColor(new Color(0x8A6A3A));
        g.setFont(getFont().deriveFont(Font.BOLD, 12f));
        g.drawString("STAGE", (int) (ox + view.sx(1, 0, 6.6)), (int) (oy + view.sy(1, 0, 6.6)));
    }

    private void drawCable(Graphics2D g, Device a, Device b) {
        double ax = a.x + 0.5, az = a.z + 0.5, bx = b.x + 0.5, bz = b.z + 0.5;
        double len = Math.hypot(bx - ax, bz - az);
        double sag = Math.min(2.5, 0.12 * len + 0.15);
        Path2D p = new Path2D.Double();
        for (int i = 0; i <= 24; i++) {
            double t = i / 24.0;
            double x = ax + (bx - ax) * t, z = az + (bz - az) * t;
            double y = 0.3 - sag * 4 * t * (1 - t) * 0.25;
            double sx = ox() + view.sx(x, Math.max(0.02, y), z), sy = oy() + view.sy(x, Math.max(0.02, y), z);
            if (i == 0) {
                p.moveTo(sx, sy);
            } else {
                p.lineTo(sx, sy);
            }
        }
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(0x0E0F11));
        g.draw(p);
        g.setStroke(new BasicStroke(1.5f));
        g.setColor(new Color(0x3A3D42));
        g.draw(p);
    }

    private void drawListener(Graphics2D g) {
        int x = (int) (ox() + view.sx(scene.listenerX, 0, scene.listenerZ));
        int y = (int) (oy() + view.sy(scene.listenerX, 0, scene.listenerZ));
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval(x - 9, y - 4, 18, 8);
        g.setColor(new Color(0x5DADE2));
        g.fillRoundRect(x - 6, y - 26, 12, 22, 6, 6);
        g.fillOval(x - 7, y - 40, 14, 14);
        g.setColor(Color.WHITE);
        g.setFont(getFont().deriveFont(Font.BOLD, 11f));
        g.drawString(scene.talking ? "YOU (talking)" : "YOU", x + 10, y - 26);
    }

    // ------------------------------------------------------------------ live parts, same positions as the mod's renderers

    /** Draws a box given in panel units of device {@code d} (u right, depth back, h up). */
    private void panelBox(Graphics2D g, Device d, double u0, double u1, double d0, double d1, double h0, double h1, int rgb) {
        double[][] c = new double[8][];
        int k = 0;
        for (double u : new double[] {u0, u1}) {
            for (double dd : new double[] {d0, d1}) {
                for (double h : new double[] {h0, h1}) {
                    double[] r = Models.rotate((16 - u) / 16, h / 16, dd / 16, d.facing.rotation, true);
                    c[k++] = new double[] {d.x + r[0], r[1], d.z + r[2]};
                }
            }
        }
        int[][] faces = {{1, 3, 7, 5}, {0, 2, 6, 4}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}};
        double[] shade = {1.0, 1.0, 0.86, 0.86, 0.72, 0.72};
        // draw the three faces that point at the camera (south, up, west in world space)
        for (int fi = 0; fi < faces.length; fi++) {
            int[] f = faces[fi];
            double nx = 0, ny = 0, nz = 0;
            double[] a = c[f[0]], b = c[f[1]], cc = c[f[2]];
            double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
            double vx = cc[0] - a[0], vy = cc[1] - a[1], vz = cc[2] - a[2];
            nx = uy * vz - uz * vy;
            ny = uz * vx - ux * vz;
            nz = ux * vy - uy * vx;
            double cx = 0, cy = 0, cz = 0;
            for (int i : f) {
                cx += c[i][0] / 4;
                cy += c[i][1] / 4;
                cz += c[i][2] / 4;
            }
            double mx = 0, my = 0, mz = 0;
            for (double[] p : c) {
                mx += p[0] / 8;
                my += p[1] / 8;
                mz += p[2] / 8;
            }
            if ((cx - mx) * nx + (cy - my) * ny + (cz - mz) * nz < 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }
            if (!(nz > 1e-9 || ny > 1e-9 || nx < -1e-9)) {
                continue;
            }
            Polygon p = new Polygon();
            for (int i : f) {
                p.addPoint((int) Math.round(ox() + view.sx(c[i][0], c[i][1], c[i][2])), (int) Math.round(oy() + view.sy(c[i][0], c[i][1], c[i][2])));
            }
            double s = ny > 1e-9 ? 1.0 : nz > 1e-9 ? 0.86 : 0.72;
            g.setColor(new Color((int) (((rgb >> 16) & 0xFF) * s), (int) (((rgb >> 8) & 0xFF) * s), (int) ((rgb & 0xFF) * s)));
            g.fillPolygon(p);
        }
    }

    private void drawLiveParts(Graphics2D g, Device d) {
        switch (d.type) {
            case MIXER -> {
                double top = MixerLayout.TOP;
                for (int i = 0; i < Device.INPUTS; i++) {
                    double u = MixerLayout.stripU(i);
                    double fd = MixerLayout.faderDepth(d.gains[i] / Device.MAX_GAIN);
                    panelBox(g, d, u - 0.75, u + 0.75, fd - 0.45, fd + 0.45, top, top + 0.9, 0xD9DCE0);
                    panelBox(g, d, u - 0.6, u + 0.6, MixerLayout.MUTE_MIN + 0.1, MixerLayout.MUTE_MAX - 0.1, top, top + 0.4, d.mutes[i] ? 0xFF3030 : 0x3A3D42);
                    panelBox(g, d, u - 0.8, u + 0.8, MixerLayout.KNOB_DEPTH - 0.8, MixerLayout.KNOB_DEPTH + 0.8, top, top + 0.8, 0x1A1B1E);
                    panelBox(g, d, u - 0.25, u + 0.25, 11.4, 11.8, top, top + 0.15, d.inputs[i] != null ? 0x3DDC84 : 0x2A2C30);
                }
                double md = MixerLayout.faderDepth(d.master / Device.MAX_GAIN);
                panelBox(g, d, MixerLayout.MASTER_U - 0.85, MixerLayout.MASTER_U + 0.85, md - 0.5, md + 0.5, top, top + 0.95, 0xD0312B);
            }
            case AMPLIFIER -> {
                panelBox(g, d, PanelLayout.Amp.SWITCH_U0, PanelLayout.Amp.SWITCH_U1, -0.4, 0, PanelLayout.Amp.SWITCH_H0, PanelLayout.Amp.SWITCH_H1, d.ampOn ? 0xE74C3C : 0x3A3D42);
                double r = PanelLayout.Amp.KNOB_R;
                panelBox(g, d, PanelLayout.Amp.KNOB_U - r, PanelLayout.Amp.KNOB_U + r, -0.9, 0, PanelLayout.Amp.KNOB_H - r, PanelLayout.Amp.KNOB_H + r, 0x9AA0A6);
                panelBox(g, d, 12.6, 13.4, -0.2, 0, 13.0, 13.8, d.ampOn ? 0x3DDC84 : 0x2A2C30);
            }
            case MEDIA_DECK -> {
                panelBox(g, d, PanelLayout.Deck.DISPLAY_U0, PanelLayout.Deck.DISPLAY_U1, -0.1, 0, PanelLayout.Deck.DISPLAY_H0, PanelLayout.Deck.DISPLAY_H1, d.playing ? 0x1F7A45 : 0x0B2A12);
                for (int i = 0; i < PanelLayout.Deck.BUTTONS.length; i++) {
                    double u = PanelLayout.Deck.buttonU0(i);
                    boolean lit = (i == 1 && d.playing) || (i == 4 && d.loop);
                    panelBox(g, d, u, u + PanelLayout.Deck.BUTTON_W, -0.4, 0, PanelLayout.Deck.BUTTON_H0, PanelLayout.Deck.BUTTON_H1, lit ? 0x2ECC71 : 0x3A3D42);
                }
            }
            case RECEIVER -> {
                double f = PanelLayout.Receiver.FRONT_DEPTH;
                panelBox(g, d, 2.5, 8.5, f - 0.1, f, 1.3, 4.0, 0x3DDC84);
                panelBox(g, d, PanelLayout.Receiver.DOWN_U0, PanelLayout.Receiver.DOWN_U0 + PanelLayout.Receiver.BUTTON_W, f - 0.4, f, PanelLayout.Receiver.BUTTON_H0, PanelLayout.Receiver.BUTTON_H1, 0x9AA0A6);
                panelBox(g, d, PanelLayout.Receiver.UP_U0, PanelLayout.Receiver.UP_U0 + PanelLayout.Receiver.BUTTON_W, f - 0.4, f, PanelLayout.Receiver.BUTTON_H0, PanelLayout.Receiver.BUTTON_H1, 0x9AA0A6);
            }
            default -> {
            }
        }
    }
}
