package dev.tonyd365.venuesound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.block.PanelMath;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws the moving parts of a device (fader caps, buttons, knob pointers, LEDs) as small coloured
 * boxes, in panel coordinates (see {@link PanelMath}): u to the operator's right, depth away from
 * the front, height up, all in 1/16 block.
 */
public final class PanelDraw {

    public static final ResourceLocation WHITE = VenueSound.id("textures/misc/white.png");
    public static final int FULL_BRIGHT = LightTexture.FULL_BRIGHT;

    private final PoseStack pose;
    private final MultiBufferSource buffers;
    private VertexConsumer vc;

    private PanelDraw(PoseStack pose, MultiBufferSource buffers) {
        this.pose = pose;
        this.buffers = buffers;
    }

    /** Pushes a pose turned to the device's facing; call {@link #end()} when done. */
    public static PanelDraw begin(PoseStack pose, MultiBufferSource buffers, Direction facing) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(PanelMath.modelRotation(facing)));
        pose.translate(-0.5, 0, -0.5);
        return new PanelDraw(pose, buffers);
    }

    public void end() {
        pose.popPose();
    }

    /** A box spanning u0-u1, d0-d1 (depth), h0-h1 (height). */
    public void box(double u0, double u1, double d0, double d1, double h0, double h1, int rgb, int light) {
        // panel -> north model frame: x = 16 - u, z = depth
        float x0 = (float) ((16 - u1) / 16), x1 = (float) ((16 - u0) / 16);
        float y0 = (float) (h0 / 16), y1 = (float) (h1 / 16);
        float z0 = (float) (d0 / 16), z1 = (float) (d1 / 16);
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        // fetch the buffer for every box: drawing text in between switches the shared buffer,
        // and a buffer kept from before would then be wrong (parts vanished or the game crashed)
        vc = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        // up
        quad(m, n, light, r, g, b, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        // north (front) and south
        quad(m, n, light, r * 8 / 10, g * 8 / 10, b * 8 / 10, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
        quad(m, n, light, r * 7 / 10, g * 7 / 10, b * 7 / 10, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
        // east and west
        quad(m, n, light, r * 7 / 10, g * 7 / 10, b * 7 / 10, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
        quad(m, n, light, r * 7 / 10, g * 7 / 10, b * 7 / 10, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
    }

    /** A box on the front face: spans u0-u1 and h0-h1, sticking out {@code depth} from the front. */
    public void frontBox(double u0, double u1, double h0, double h1, double depth, int rgb, int light) {
        box(u0, u1, -depth, 0.02, h0, h1, rgb, light);
    }

    private void quad(Matrix4f m, Matrix3f n, int light, int r, int g, int b, float nx, float ny, float nz,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz) {
        v(m, n, light, r, g, b, nx, ny, nz, ax, ay, az, 0, 0);
        v(m, n, light, r, g, b, nx, ny, nz, bx, by, bz, 0, 1);
        v(m, n, light, r, g, b, nx, ny, nz, cx, cy, cz, 1, 1);
        v(m, n, light, r, g, b, nx, ny, nz, dx, dy, dz, 1, 0);
    }

    private void v(Matrix4f m, Matrix3f n, int light, int r, int g, int b, float nx, float ny, float nz,
                   float x, float y, float z, float u, float vv) {
        //? if >=1.21 {
        /*vc.addVertex(m, x, y, z).setColor(r, g, b, 255).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(pose.last(), nx, ny, nz);
        *///?} else {
        vc.vertex(m, x, y, z).color(r, g, b, 255).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, nx, ny, nz).endVertex();
        //?}
    }

    /**
     * Writes text on the front face, starting at the top-left corner (u, h), {@code size} units
     * tall, at {@code depth} from the block's front edge.
     */
    public void frontText(MultiBufferSource buffers, String text, double u, double h, double depth, double size, int argb) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        pose.pushPose();
        pose.translate((16 - u) / 16, h / 16, depth / 16 - 0.003);
        pose.mulPose(Axis.YP.rotationDegrees(180));
        float k = (float) (size / 8.0 / 16.0);
        pose.scale(k, -k, k);
        font.drawInBatch(text, 0, 0, argb, false, pose.last().pose(), buffers,
                net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, 0, FULL_BRIGHT);
        pose.popPose();
    }

    /** Text lying on a top surface at height {@code h}, readable by the operator, top-left at (u, depth). */
    public void topText(MultiBufferSource buffers, String text, double u, double depth, double h, double size, int argb) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        pose.pushPose();
        pose.translate((16 - u) / 16, h / 16 + 0.002, depth / 16);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.mulPose(Axis.YP.rotationDegrees(180));
        float k = (float) (size / 8.0 / 16.0);
        pose.scale(k, -k, k);
        font.drawInBatch(text, 0, 0, argb, false, pose.last().pose(), buffers,
                net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, 0, FULL_BRIGHT);
        pose.popPose();
    }

    /** Like {@link #frontText} but on the back face, readable from behind the device. */
    public void backText(MultiBufferSource buffers, String text, double u, double h, double depth, double size, int argb) {
        net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
        pose.pushPose();
        pose.translate((16 - u) / 16, h / 16, depth / 16 + 0.003);
        float k = (float) (size / 8.0 / 16.0);
        pose.scale(k, -k, k);
        font.drawInBatch(text, 0, 0, argb, false, pose.last().pose(), buffers,
                net.minecraft.client.gui.Font.DisplayMode.POLYGON_OFFSET, 0, FULL_BRIGHT);
        pose.popPose();
    }

    /** Draws every socket of a device with its label. */
    public void sockets(MultiBufferSource buffers, String deviceKey, int light) {
        for (dev.tonyd365.venuesound.block.PortLayout.Port p : dev.tonyd365.venuesound.block.PortLayout.ports(deviceKey)) {
            if (p.face() == dev.tonyd365.venuesound.block.PortLayout.Face.ANY) {
                continue;
            }
            int ring = switch (p.dir()) {
                case IN -> 0x2E86C1;
                case OUT -> 0xC0392B;
                case BOTH -> 0x9AA0A6;
            };
            double cu = p.u(), ch = p.h();
            double r = Math.min(p.u1() - p.u0(), p.h1() - p.h0()) / 2;
            boolean front = p.face() == dev.tonyd365.venuesound.block.PortLayout.Face.FRONT;
            double d0 = front ? p.depth() - 0.35 : p.depth();
            double d1 = front ? p.depth() : p.depth() + 0.35;
            double di0 = front ? p.depth() - 0.45 : p.depth() + 0.25;
            double di1 = front ? p.depth() - 0.25 : p.depth() + 0.45;
            box(cu - r, cu + r, d0, d1, ch - r, ch + r, ring, light);
            // the face of the connector, so XLR, TRS, Speakon and multipin look different
            switch (dev.tonyd365.venuesound.block.PortLayout.connector(deviceKey, p)) {
                case TRS -> {
                    box(cu - r * 0.55, cu + r * 0.55, di0, di1, ch - r * 0.55, ch + r * 0.55, 0x2A2C30, light);
                    box(cu - r * 0.22, cu + r * 0.22, di0 - 0.05, di1, ch - r * 0.22, ch + r * 0.22, 0x050506, light);
                }
                case XLR, COMBO -> {
                    box(cu - r * 0.62, cu + r * 0.62, di0, di1, ch - r * 0.62, ch + r * 0.62, 0x0A0A0B, light);
                    // three pins of an XLR (a combo jack also has the TRS hole in the middle)
                    double pr = Math.max(0.08, r * 0.1);
                    for (double[] pin : new double[][] {{-0.3, 0.22}, {0.3, 0.22}, {0, -0.3}}) {
                        double pu = cu + pin[0] * r, ph = ch + pin[1] * r;
                        box(pu - pr, pu + pr, di0 - 0.05, di1, ph - pr, ph + pr, 0xB8BCC2, light);
                    }
                }
                case SPEAKON -> {
                    box(cu - r * 0.7, cu + r * 0.7, di0, di1, ch - r * 0.7, ch + r * 0.7, 0x1F3550, light);
                    box(cu - r * 0.32, cu + r * 0.32, di0 - 0.05, di1, ch - r * 0.32, ch + r * 0.32, 0x050506, light);
                }
                case MULTIPIN -> {
                    box(cu - r * 0.8, cu + r * 0.8, di0, di1, ch - r * 0.45, ch + r * 0.45, 0x0A0A0B, light);
                    for (int k = -3; k <= 3; k++) {
                        double pu = cu + k * r * 0.2;
                        box(pu - 0.06, pu + 0.06, di0 - 0.05, di1, ch - 0.06, ch + 0.06, 0xB8BCC2, light);
                    }
                }
                default -> box(cu - r * 0.55, cu + r * 0.55, di0, di1, ch - r * 0.55, ch + r * 0.55, 0x0A0A0B, light);
            }
            double size = Math.max(0.6, Math.min(1.0, r * 0.7));
            if (front) {
                frontText(buffers, p.label(), p.u0() - 0.1, p.h1() + size + 0.2, p.depth(), size, 0xFFDDDDDD);
            } else {
                backText(buffers, p.label(), p.u1() + 0.1, p.h1() + size + 0.2, p.depth(), size, 0xFFDDDDDD);
            }
        }
    }

    /**
     * Where a cable attaches to a socket, in the block-local space of the device's own position.
     */
    public static org.joml.Vector3f anchor(net.minecraft.core.Direction facing, dev.tonyd365.venuesound.block.PortLayout.Port port) {
        net.minecraft.world.phys.Vec3 v = PanelMath.socket(facing, port);
        return new org.joml.Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    public PoseStack pose() {
        return pose;
    }
}
