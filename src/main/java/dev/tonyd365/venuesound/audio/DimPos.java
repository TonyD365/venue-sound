package dev.tonyd365.venuesound.audio;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** A block position in a specific dimension. Immutable, safe to share across threads. */
public record DimPos(ResourceKey<Level> dimension, BlockPos pos) {

    public double distanceSqrTo(double x, double y, double z) {
        return pos.distToCenterSqr(x, y, z);
    }
}
