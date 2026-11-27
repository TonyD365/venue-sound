package dev.tonyd365.venuesound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
import dev.tonyd365.venuesound.block.DeviceBlock;
import dev.tonyd365.venuesound.block.PortLayout;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Draws every cable a device sends out as a thick black wire that sags between the two blocks. */
public class CableRenderer<T extends AudioNodeBlockEntity> implements BlockEntityRenderer<T> {

    private static final int SEGMENTS = 20;
    private static final float HALF_WIDTH = 0.035F;

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        if (be.getBlockState().hasProperty(DeviceBlock.FACING)) {
            PanelDraw d = PanelDraw.begin(pose, buffers, be.getBlockState().getValue(DeviceBlock.FACING));
            d.sockets(buffers, be.deviceKey(), light);
            renderPanel(be, d, buffers, light);
            d.end();
        }
        if (be.getLinks().isEmpty()) {
            return;
        }
        Level level = be.getLevel();
        BlockPos origin = be.getBlockPos();
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f m = pose.last().pose();
        Vec3 base = Vec3.atLowerCornerOf(origin);
        for (AudioNodeBlockEntity.Link link : be.getLinks()) {
            java.util.List<Vec3> points = new java.util.ArrayList<>();
            points.add(socket(level, origin, be.deviceKey(), PortLayout.Dir.OUT, link.fromPort()));
            for (BlockPos clip : link.via()) {
                BlockState cs = level.getBlockState(clip);
                if (cs.getBlock() instanceof dev.tonyd365.venuesound.block.CableClipBlock) {
                    points.add(dev.tonyd365.venuesound.block.CableClipBlock.anchor(cs, clip));
                }
            }
            String targetKey = level.getBlockEntity(link.target()) instanceof AudioNodeBlockEntity t ? t.deviceKey() : "";
            points.add(socket(level, link.target(), targetKey, PortLayout.Dir.IN, link.toPort()));
            drawRun(level, vc, m, points, base);
        }
    }

    /** World position of a socket on the device at {@code pos}. */
    private static Vec3 socket(Level level, BlockPos pos, String key, PortLayout.Dir dir, int index) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(DeviceBlock.FACING)) {
            return Vec3.atCenterOf(pos);
        }
        org.joml.Vector3f a = PanelDraw.anchor(state.getValue(DeviceBlock.FACING), PortLayout.byIndex(key, dir, index));
        return new Vec3(pos.getX() + a.x(), pos.getY() + a.y(), pos.getZ() + a.z());
    }

    /** Extra live parts drawn on the device, in panel coordinates. */
    protected void renderPanel(T be, PanelDraw d, MultiBufferSource buffers, int light) {
    }

    /** Draws a cable through {@code points} (sockets and clips), hanging and draped over obstacles. */
    public static void drawRun(Level level, VertexConsumer vc, Matrix4f m, java.util.List<Vec3> points, Vec3 base) {
        for (int i = 0; i + 1 < points.size(); i++) {
            Vec3 a = points.get(i), b = points.get(i + 1);
            double sag = Math.min(2.5D, 0.12D * a.distanceTo(b) + 0.15D);
            java.util.List<Vec3> path = dev.tonyd365.venuesound.block.CableRoute.path(level, a, b, sag);
            if (path == null) {
                path = java.util.List.of(a, b);
            }
            drawPath(vc, m, path, base);
        }
    }

    private static void drawPath(VertexConsumer vc, Matrix4f m, java.util.List<Vec3> path, Vec3 base) {
        for (int i = 0; i + 1 < path.size(); i++) {
            Vec3 p0 = path.get(i).subtract(base), p1 = path.get(i + 1).subtract(base);
            Vec3 d = p1.subtract(p0);
            double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
            // two crossed ribbons so the cable looks round from every angle
            Vec3 side = horizontal < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(-d.z / horizontal, 0, d.x / horizontal);
            int shade = (i % 2 == 0) ? 22 : 30;
            quad(vc, m, p0, p1, side.scale(HALF_WIDTH), shade);
            quad(vc, m, p0, p1, new Vec3(0, HALF_WIDTH, 0), shade);
        }
    }

    private static Vec3 point(Vec3 a, Vec3 d, double sag, float t) {
        return new Vec3(a.x + d.x * t, a.y + d.y * t - sag * 4 * t * (1 - t), a.z + d.z * t);
    }

    private static void quad(VertexConsumer vc, Matrix4f m, Vec3 p0, Vec3 p1, Vec3 off, int shade) {
        vertex(vc, m, p0.subtract(off), shade);
        vertex(vc, m, p0.add(off), shade);
        vertex(vc, m, p1.add(off), shade);
        vertex(vc, m, p1.subtract(off), shade);
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Vec3 p, int shade) {
        vc.vertex(m, (float) p.x, (float) p.y, (float) p.z).color(shade, shade, shade + 4, 255).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(T be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
