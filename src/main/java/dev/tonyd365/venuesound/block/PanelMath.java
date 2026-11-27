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

    /** Clockwise blockstate rotation of a device facing {@code facing} (north 0, east 90, south 180, west 270). */
    public static int stateRotation(Direction facing) {
        return switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    /** Block-local position of panel point (u, height, depth) of a device facing {@code facing}. */
    public static Vec3 toBlock(Direction facing, double u, double height, double depth) {
        double dx = (16 - u) / 16 - 0.5, dz = depth / 16 - 0.5;
        for (int r = 0; r < stateRotation(facing) / 90; r++) {
            double nx = -dz;
            dz = dx;
            dx = nx;
        }
        return new Vec3(0.5 + dx, height / 16, 0.5 + dz);
    }

    /** Block-local point where a cable leaves or enters a socket. */
    public static Vec3 socket(Direction facing, PortLayout.Port port) {
        if (port == null) {
            return toBlock(facing, 8, 4, 8);
        }
        if (port.face() == PortLayout.Face.ANY) {
            return toBlock(facing, 8, 2.0, 8); // the stand mic's socket, on the pole just above its base
        }
        double d = port.face() == PortLayout.Face.FRONT ? port.depth() - 0.5 : port.depth() + 0.5;
        return toBlock(facing, port.u(), port.h(), d);
    }

    /** Rotation (degrees about +Y) that turns the north-facing model frame to {@code facing}. */
    public static float modelRotation(Direction facing) {
        return 180F - facing.toYRot();
    }
}
