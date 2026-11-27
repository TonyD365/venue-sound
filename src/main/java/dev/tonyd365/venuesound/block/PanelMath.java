package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Converts a click on a device into panel coordinates, in model units (1/16 block), as the
 * operator standing in front of the device sees it:
 * {@code u} grows to the operator's right, {@code depth} grows away from the front edge,
 * {@code height} grows up.
 */
public final class PanelMath {

    private PanelMath() {
    }

    public record Point(double u, double depth, double height) {
    }

    /** @param facing the direction the device's front points to (towards the operator) */
    public static Point local(BlockPos pos, Direction facing, Vec3 hit) {
        double x = hit.x - pos.getX();
        double y = hit.y - pos.getY();
        double z = hit.z - pos.getZ();
        double u;
        double d;
        switch (facing) {
            case SOUTH -> {
                u = x;
                d = 1 - z;
            }
            case EAST -> {
                u = 1 - z;
                d = 1 - x;
            }
            case WEST -> {
                u = z;
                d = x;
            }
            default -> { // NORTH
                u = 1 - x;
                d = z;
            }
        }
        return new Point(u * 16, d * 16, y * 16);
    }

    /** Rotation (degrees about +Y) that turns the north-facing model frame to {@code facing}. */
    public static float modelRotation(Direction facing) {
        return 180F - facing.toYRot();
    }
}
