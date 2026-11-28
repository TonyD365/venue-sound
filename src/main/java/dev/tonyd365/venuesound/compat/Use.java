package dev.tonyd365.venuesound.compat;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

/**
 * Right-click results on every Minecraft version. The blocks answer a right-click with an
 * InteractionResult from {@code onUse} (see DeviceBlock); since 1.20.5 that is handed to
 * Minecraft from {@code useItemOn}, so a PASS still lets the held item (a cable) act.
 */
public final class Use {

    private Use() {
    }

    /** "Handled": SUCCESS on the client, CONSUME on the server. */
    public static InteractionResult sided(Level level) {
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    //? if >=1.20.5 {
    /*/^* What useItemOn returns for a block's onUse result: PASS goes on to the held item. ^/
    public static net.minecraft.world.ItemInteractionResult itemOn(InteractionResult r, Level level) {
        if (r.consumesAction()) {
            return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return r == InteractionResult.FAIL ? net.minecraft.world.ItemInteractionResult.FAIL
                : net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
    *///?}
}
