package dev.tonyd365.venuesound.workbench;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the mod's block models (assets/venuesound/models/block/*.json) and draws them face by
 * face with their textures and UVs, rotated like the blockstate files rotate them.
 */
public final class Models {

    /** Projection used everywhere in the Workbench: front (south) faces are drawn flat. */
    public static final class View {
        public final double px;          // pixels per block
        public static final double KX = -0.38, KY = -0.5; // how one block of depth (towards north) shifts the image

        public View(double px) {
            this.px = px;
        }

        /** Screen x of a world point, relative to the scene origin. */
        public double sx(double x, double y, double z) {
            return (x + (Scene.DEPTH - z) * KX) * px;
        }

        public double sy(double x, double y, double z) {
            return (-y + (Scene.DEPTH - z) * KY) * px;
        }
    }

    private record Face(double[][] corners, double[] uv, BufferedImage tex, double shade, double depth) {
    }

    private final Map<String, BufferedImage> textures = new HashMap<>();
    private final Map<String, JsonObject> models = new HashMap<>();

    private static InputStream resource(String path) throws IOException {
        InputStream in = Models.class.getResourceAsStream("/assets/venuesound/" + path);
        if (in == null) {
            throw new IOException("Missing resource " + path);
        }
        return in;
    }

    public BufferedImage texture(String ref) {
        String name = ref.contains(":") ? ref.substring(ref.indexOf(':') + 1) : ref;
        return textures.computeIfAbsent(name, n -> {
            try (InputStream in = resource("textures/" + n + ".png")) {
                return ImageIO.read(in);
            } catch (IOException e) {
                BufferedImage missing = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
                missing.setRGB(0, 0, 0xFFFF00FF);
                missing.setRGB(1, 1, 0xFFFF00FF);
                return missing;
            }
        });
    }

    private JsonObject model(String name) {
        return models.computeIfAbsent(name, n -> {
            try (InputStream in = resource("models/block/" + n + ".json")) {
                return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /**
     * Draws model {@code name} for a block whose lower north-west corner is at world (bx, by, bz),
     * rotated clockwise by {@code rotation} degrees.
     */
    public void draw(Graphics2D g, View v, double ox, double oy, String name, int rotation, double bx, double by, double bz) {
        JsonObject m = model(name);
        Map<String, String> tex = new HashMap<>();
        if (m.has("textures")) {
            for (Map.Entry<String, JsonElement> e : m.getAsJsonObject("textures").entrySet()) {
                tex.put(e.getKey(), e.getValue().getAsString());
            }
        }
        JsonArray elements;
        if ("block/orientable".equals(m.has("parent") ? m.get("parent").getAsString() : "")) {
            elements = JsonParser.parseString("[{\"from\":[0,0,0],\"to\":[16,16,16],\"faces\":{"
                    + "\"north\":{\"texture\":\"#front\"},\"south\":{\"texture\":\"#side\"},\"east\":{\"texture\":\"#side\"},"
                    + "\"west\":{\"texture\":\"#side\"},\"up\":{\"texture\":\"#top\"},\"down\":{\"texture\":\"#top\"}}}]").getAsJsonArray();
        } else {
            elements = m.getAsJsonArray("elements");
        }
        List<Face> faces = new ArrayList<>();
        for (JsonElement el : elements) {
            JsonObject e = el.getAsJsonObject();
            double[] f = nums(e.getAsJsonArray("from"));
            double[] t = nums(e.getAsJsonArray("to"));
            JsonObject fc = e.getAsJsonObject("faces");
            for (Map.Entry<String, JsonElement> entry : fc.entrySet()) {
                String dir = entry.getKey();
                JsonObject face = entry.getValue().getAsJsonObject();
                double[] n = normal(dir);
                double[] rn = rotate(n[0], n[1], n[2], rotation, false);
                // visible from the Workbench camera: south, up and west faces
                if (!(rn[2] > 0.5 || rn[1] > 0.5 || rn[0] < -0.5)) {
                    continue;
                }
                double[][] c = corners(dir, f, t);
                double[][] world = new double[4][];
                double cz = 0, cy = 0, cx = 0;
                for (int i = 0; i < 4; i++) {
                    double[] r = rotate(c[i][0] / 16, c[i][1] / 16, c[i][2] / 16, rotation, true);
                    world[i] = new double[] {bx + r[0], by + r[1], bz + r[2]};
                    cx += world[i][0] / 4;
                    cy += world[i][1] / 4;
                    cz += world[i][2] / 4;
                }
                double[] uv = face.has("uv") ? nums(face.getAsJsonArray("uv")) : defaultUv(dir, f, t);
                String ref = face.get("texture").getAsString();
                while (ref.startsWith("#")) {
                    ref = tex.getOrDefault(ref.substring(1), "venuesound:block/missing");
                }
                double shade = rn[1] > 0.5 ? 1.0 : rn[2] > 0.5 ? 0.86 : 0.7;
                faces.add(new Face(world, uv, texture(ref), shade, cz - cx * 0.4 + cy * 0.3));
            }
        }
        faces.sort(Comparator.comparingDouble(Face::depth));
        for (Face face : faces) {
            drawFace(g, v, ox, oy, face);
        }
    }

    private static void drawFace(Graphics2D g, View v, double ox, double oy, Face f) {
        double u0 = f.uv[0], v0 = f.uv[1], u1 = f.uv[2], v1 = f.uv[3];
        BufferedImage tex = f.tex;
        int tw = tex.getWidth(), th = tex.getHeight();
        int nu = Math.max(1, (int) Math.round(Math.abs(u1 - u0) * tw / 16));
        int nv = Math.max(1, (int) Math.round(Math.abs(v1 - v0) * th / 16));
        double[][] c = f.corners;
        for (int i = 0; i < nu; i++) {
            for (int j = 0; j < nv; j++) {
                double tu = u0 + (u1 - u0) * (i + 0.5) / nu;
                double tv = v0 + (v1 - v0) * (j + 0.5) / nv;
                int argb = tex.getRGB(Math.min(tw - 1, Math.max(0, (int) (tu * tw / 16))), Math.min(th - 1, Math.max(0, (int) (tv * th / 16))));
                if ((argb >>> 24) < 10) {
                    continue;
                }
                double s0 = (double) i / nu, s1 = (double) (i + 1) / nu, t0 = (double) j / nv, t1 = (double) (j + 1) / nv;
                Polygon p = new Polygon();
                for (double[] st : new double[][] {{s0, t0}, {s1, t0}, {s1, t1}, {s0, t1}}) {
                    double[] w = bilerp(c, st[0], st[1]);
                    p.addPoint((int) Math.round(ox + v.sx(w[0], w[1], w[2])), (int) Math.round(oy + v.sy(w[0], w[1], w[2])));
                }
                g.setColor(shade(argb, f.shade));
                g.fillPolygon(p);
                g.drawPolygon(p);
            }
        }
    }

    private static Color shade(int argb, double f) {
        int r = (int) Math.min(255, ((argb >> 16) & 0xFF) * f);
        int gg = (int) Math.min(255, ((argb >> 8) & 0xFF) * f);
        int b = (int) Math.min(255, (argb & 0xFF) * f);
        return new Color(r, gg, b);
    }

    /** Corner 0 = (u0,v0), 1 = (u1,v0), 2 = (u1,v1), 3 = (u0,v1). */
    private static double[] bilerp(double[][] c, double s, double t) {
        double[] top = lerp(c[0], c[1], s);
        double[] bottom = lerp(c[3], c[2], s);
        return lerp(top, bottom, t);
    }

    private static double[] lerp(double[] a, double[] b, double t) {
        return new double[] {a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t};
    }

    private static double[] nums(JsonArray a) {
        double[] out = new double[a.size()];
        for (int i = 0; i < a.size(); i++) {
            out[i] = a.get(i).getAsDouble();
        }
        return out;
    }

    private static double[] normal(String dir) {
        return switch (dir) {
            case "north" -> new double[] {0, 0, -1};
            case "south" -> new double[] {0, 0, 1};
            case "east" -> new double[] {1, 0, 0};
            case "west" -> new double[] {-1, 0, 0};
            case "up" -> new double[] {0, 1, 0};
            default -> new double[] {0, -1, 0};
        };
    }

    /** Face corners in model units, as seen from outside: top-left, top-right, bottom-right, bottom-left. */
    private static double[][] corners(String dir, double[] f, double[] t) {
        double x1 = f[0], y1 = f[1], z1 = f[2], x2 = t[0], y2 = t[1], z2 = t[2];
        return switch (dir) {
            case "north" -> new double[][] {{x2, y2, z1}, {x1, y2, z1}, {x1, y1, z1}, {x2, y1, z1}};
            case "south" -> new double[][] {{x1, y2, z2}, {x2, y2, z2}, {x2, y1, z2}, {x1, y1, z2}};
            case "west" -> new double[][] {{x1, y2, z1}, {x1, y2, z2}, {x1, y1, z2}, {x1, y1, z1}};
            case "east" -> new double[][] {{x2, y2, z2}, {x2, y2, z1}, {x2, y1, z1}, {x2, y1, z2}};
            case "up" -> new double[][] {{x1, y2, z1}, {x2, y2, z1}, {x2, y2, z2}, {x1, y2, z2}};
            default -> new double[][] {{x1, y1, z2}, {x2, y1, z2}, {x2, y1, z1}, {x1, y1, z1}};
        };
    }

    private static double[] defaultUv(String dir, double[] f, double[] t) {
        double x1 = f[0], y1 = f[1], z1 = f[2], x2 = t[0], y2 = t[1], z2 = t[2];
        return switch (dir) {
            case "north" -> new double[] {16 - x2, 16 - y2, 16 - x1, 16 - y1};
            case "south" -> new double[] {x1, 16 - y2, x2, 16 - y1};
            case "west" -> new double[] {z1, 16 - y2, z2, 16 - y1};
            case "east" -> new double[] {16 - z2, 16 - y2, 16 - z1, 16 - y1};
            case "up" -> new double[] {x1, z1, x2, z2};
            default -> new double[] {x1, 16 - z2, x2, 16 - z1};
        };
    }

    /**
     * Rotates clockwise (seen from above) by {@code degrees}. Points rotate about the block centre;
     * directions ({@code point == false}) about the origin.
     */
    public static double[] rotate(double x, double y, double z, int degrees, boolean point) {
        double cx = point ? 0.5 : 0, cz = point ? 0.5 : 0;
        double dx = x - cx, dz = z - cz;
        for (int r = 0; r < ((degrees / 90) % 4 + 4) % 4; r++) {
            double nx = -dz, nz = dx;
            dx = nx;
            dz = nz;
        }
        return new double[] {cx + dx, y, cz + dz};
    }

    public static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }
}
