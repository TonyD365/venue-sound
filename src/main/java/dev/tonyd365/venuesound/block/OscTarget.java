package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * The console a player's OSC controller works on: the last console they used (client side). Kept
 * in common code so the console block can set it without referring to client classes.
 */
public final class OscTarget {

    @Nullable
    private static volatile BlockPos console;

    private OscTarget() {
    }

    public static void touched(BlockPos pos) {
        console = pos.immutable();
    }

    @Nullable
    public static BlockPos console() {
        return console;
    }
}
