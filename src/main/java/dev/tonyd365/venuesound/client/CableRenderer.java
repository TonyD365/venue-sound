package dev.tonyd365.venuesound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
import dev.tonyd365.venuesound.block.PaSpeakerBlock;
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
        if (be.getOutputs().isEmpty() || be.getLevel() == null) {
            return;
        }
        Level level = be.getLevel();
        BlockPos origin = be.getBlockPos();
        Vec3 start = anchor(level, origin).subtract(Vec3.atLowerCornerOf(origin));
        VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
        Matrix4f m = pose.last().pose();
        for (BlockPos target : be.getOutputs()) {
            Vec3 end = anchor(level, target).subtract(Vec3.atLowerCornerOf(origin));
            drawCable(vc, m, start, end);
        }
    }

    /** Where a cable attaches on a device: low on the block, or on the cabinet base for PA speakers. */
    private static Vec3 anchor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        double y = state.getBlock() instanceof PaSpeakerBlock ? 0.25D : 0.3D;
        return new Vec3(pos.getX() + 0.5D, pos.getY() + y, pos.getZ() + 0.5D);
    }

    private static void drawCable(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b) {
        Vec3 d = b.subtract(a);
        double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
        double sag = Math.min(2.5D, 0.12D * d.length() + 0.15D);
        // Two crossed ribbons so the cable looks round from every angle.
        Vec3 side = horizontal < 1.0E-4 ? new Vec3(1, 0, 0) : new Vec3(-d.z / horizontal, 0, d.x / horizontal);
        Vec3[] offsets = {side.scale(HALF_WIDTH), new Vec3(0, HALF_WIDTH, 0)};
        for (Vec3 off : offsets) {
            Vec3 prev = point(a, d, sag, 0);
            for (int i = 1; i <= SEGMENTS; i++) {
                Vec3 next = point(a, d, sag, i / (float) SEGMENTS);
                int shade = (i % 2 == 0) ? 22 : 30;
                quad(vc, m, prev, next, off, shade);
                prev = next;
            }
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
