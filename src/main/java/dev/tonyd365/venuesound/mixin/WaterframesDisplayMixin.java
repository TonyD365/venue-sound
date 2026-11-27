package dev.tonyd365.venuesound.mixin;

import dev.tonyd365.venuesound.client.CaptureGain;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional WATERFrAMES compat: a screen inside a Video Capture zone takes its volume from the
 * console path (faders, mute, amp, speakers on) and the player's distance to the nearest reached
 * speaker. Skipped when WATERFrAMES is not installed.
 */
@Pseudo
@Mixin(targets = "me.srrapero720.waterframes.client.display.Display", remap = false)
public class WaterframesDisplayMixin {

    @Inject(method = "rangedVol", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void venuesound$consoleVolume(int volume, int min, int max, CallbackInfoReturnable<Integer> cir) {
        try {
            java.lang.reflect.Field f = getClass().getDeclaredField("tile");
            f.setAccessible(true);
            if (f.get(this) instanceof BlockEntity tile) {
                Float gain = CaptureGain.gainAt(tile.getBlockPos());
                if (gain != null) {
                    cir.setReturnValue(Math.max(0, Math.min(100, Math.round(volume * gain))));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // WATERFrAMES changed: leave its own volume alone
        }
    }
}
