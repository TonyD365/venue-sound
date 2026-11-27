package dev.tonyd365.venuesound.mixin;

import dev.tonyd365.venuesound.block.ScrollControl;
import dev.tonyd365.venuesound.net.ScrollPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The mouse wheel turns the knob or fader under the crosshair instead of changing hotbar slot. */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void venuesound$scroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (window != mc.getWindow().getWindow() || mc.screen != null || mc.player == null || mc.level == null || yOffset == 0
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != BlockHitResult.Type.BLOCK) {
            return;
        }
        if (ScrollControl.find(mc.level, hit.getBlockPos(), hit.getDirection(), hit.getLocation()) == null) {
            return;
        }
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(hit.getBlockPos());
        buf.writeEnum(hit.getDirection());
        buf.writeDouble(hit.getLocation().x);
        buf.writeDouble(hit.getLocation().y);
        buf.writeDouble(hit.getLocation().z);
        buf.writeByte(yOffset > 0 ? 1 : -1);
        ClientPlayNetworking.send(ScrollPackets.SCROLL, buf);
        ci.cancel();
    }
}
