package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Set;

/** Checks that a cable run does not pass through solid blocks, and how far it may sag. */
public final class CableRoute {

    private CableRoute() {
    }

    /** True if the straight line a-b crosses no solid block other than the {@code ignore} positions. */
    public static boolean clear(BlockGetter level, Vec3 a, Vec3 b, Set<BlockPos> ignore) {
        double len = a.distanceTo(b);
        int steps = Math.max(2, (int) Math.ceil(len / 0.2));
        for (int i = 1; i < steps; i++) {
            Vec3 p = a.lerp(b, i / (double) steps);
            if (solidAt(level, p, ignore)) {
                return false;
            }
        }
        return true;
    }

    /** Whether point {@code p} is inside a block's collision shape (cables rest on, never inside, blocks). */
    public static boolean solidAt(BlockGetter level, Vec3 p, Set<BlockPos> ignore) {
        BlockPos pos = BlockPos.containing(p);
        if (ignore.contains(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        // cables may cross devices and clips (they plug into them), never walls, floors or other blocks
        if (state.isAir() || state.getBlock() instanceof DeviceBlock || state.getBlock() instanceof CableClipBlock) {
            return false;
        }
        VoxelShape shape = state.getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return false;
        }
        Vec3 local = p.subtract(pos.getX(), pos.getY(), pos.getZ());
        for (var box : shape.toAabbs()) {
            if (box.inflate(0.02).contains(local)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The largest sag (up to {@code wanted}) for which the hanging cable a-b stays out of solid
     * blocks: like a real cable that comes to rest on a floor or ledge instead of sinking into it.
     */
    public static double sag(BlockGetter level, Vec3 a, Vec3 b, double wanted, Set<BlockPos> ignore) {
        for (double s = wanted; s > 0.01; s *= 0.6) {
            boolean ok = true;
            for (int i = 1; i < 16 && ok; i++) {
                double t = i / 16.0;
                Vec3 p = a.lerp(b, t).subtract(0, s * 4 * t * (1 - t), 0);
                ok = !solidAt(level, p, ignore);
            }
            if (ok) {
                return s;
            }
        }
        return 0;
    }
}
