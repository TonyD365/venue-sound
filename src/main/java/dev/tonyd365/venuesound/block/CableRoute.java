package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * How a cable lies between two points: it hangs with some sag and rests on top of whatever it
 * crosses, like a real cable draped over a step or a ledge. It cannot go through anything taller
 * than {@link #MAX_DRAPE} above its straight line: that is a wall, and needs a cable clip.
 */
public final class CableRoute {

    /** How far a cable may be lifted over an obstacle before the obstacle counts as a wall. */
    public static final double MAX_DRAPE = 1.1;
    /** Length at each end that is the plug going into its socket or clip (not checked). */
    private static final double PLUG = 0.45;
    private static final int SEGMENTS = 24;

    private CableRoute() {
    }

    /**
     * The points the cable passes through from {@code a} to {@code b} with up to {@code sag}
     * blocks of sag, lifted over low obstacles; null if a wall is in the way.
     */
    public static List<Vec3> path(BlockGetter level, Vec3 a, Vec3 b, double sag) {
        double len = a.distanceTo(b);
        List<Vec3> points = new ArrayList<>(SEGMENTS + 1);
        for (int i = 0; i <= SEGMENTS; i++) {
            double t = i / (double) SEGMENTS;
            Vec3 straight = a.lerp(b, t);
            Vec3 p = straight.subtract(0, sag * 4 * t * (1 - t), 0);
            if (t * len >= PLUG && (1 - t) * len >= PLUG) {
                double top = surfaceAbove(level, p);
                if (!Double.isNaN(top)) {
                    if (top - straight.y > MAX_DRAPE) {
                        return null;
                    }
                    p = new Vec3(p.x, top + 0.04, p.z);
                }
            }
            points.add(p);
        }
        return points;
    }

    /** True if a cable can run from a to b (no wall in between). */
    public static boolean clear(BlockGetter level, Vec3 a, Vec3 b) {
        return path(level, a, b, 0) != null;
    }

    /**
     * If {@code p} is inside a solid block, the height of the top of that solid stack (where a
     * cable would come to rest); otherwise NaN.
     */
    private static double surfaceAbove(BlockGetter level, Vec3 p) {
        BlockPos pos = BlockPos.containing(p);
        double top = solidTop(level, pos, p);
        if (Double.isNaN(top)) {
            return Double.NaN;
        }
        // climb up through solid blocks stacked on top
        for (int i = 1; i <= 3; i++) {
            BlockPos up = pos.above(i);
            double t = solidTop(level, up, new Vec3(p.x, up.getY() + 0.01, p.z));
            if (Double.isNaN(t)) {
                break;
            }
            top = t;
        }
        return top;
    }

    /** Top of the block's collision shape at (p.x, p.z) if p is inside it, else NaN. */
    private static double solidTop(BlockGetter level, BlockPos pos, Vec3 p) {
        BlockState state = level.getBlockState(pos);
        // cables may cross devices and clips (they plug into them)
        if (state.isAir() || state.getBlock() instanceof DeviceBlock || state.getBlock() instanceof CableClipBlock) {
            return Double.NaN;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return Double.NaN;
        }
        Vec3 local = p.subtract(pos.getX(), pos.getY(), pos.getZ());
        double top = Double.NaN;
        for (var box : shape.toAabbs()) {
            if (box.inflate(0.02).contains(local)) {
                top = Double.isNaN(top) ? box.maxY : Math.max(top, box.maxY);
            }
        }
        return Double.isNaN(top) ? Double.NaN : pos.getY() + top;
    }
}
