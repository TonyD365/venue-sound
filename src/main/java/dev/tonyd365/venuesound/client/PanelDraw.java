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
    private final VertexConsumer vc;

    private PanelDraw(PoseStack pose, MultiBufferSource buffers) {
        this.pose = pose;
        this.vc = buffers.getBuffer(RenderType.entitySolid(WHITE));
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
        vc.vertex(m, x, y, z).color(r, g, b, 255).uv(u, vv).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, nx, ny, nz).endVertex();
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

    public PoseStack pose() {
        return pose;
    }
}
