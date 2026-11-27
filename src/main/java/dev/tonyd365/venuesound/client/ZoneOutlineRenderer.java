package dev.tonyd365.venuesound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.tonyd365.venuesound.item.ZoneSelectorItem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** While a Zone Selector is held, outlines the selected box (or the box to the targeted block). */
public final class ZoneOutlineRenderer {

    private ZoneOutlineRenderer() {
    }

    public static void render(WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || ctx.consumers() == null) {
            return;
        }
        ItemStack stack = mc.player.getMainHandItem().getItem() instanceof ZoneSelectorItem ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
        if (!(stack.getItem() instanceof ZoneSelectorItem)) {
            return;
        }
        BlockPos a = ZoneSelectorItem.corner(stack, "A"), b = ZoneSelectorItem.corner(stack, "B");
        if (a == null) {
            return;
        }
        if (b == null && mc.hitResult instanceof BlockHitResult hit && hit.getType() == BlockHitResult.Type.BLOCK) {
            b = hit.getBlockPos();
        }
        AABB box = new AABB(a).minmax(new AABB(b == null ? a : b));
        boolean ok = box.getXsize() <= ZoneSelectorItem.MAX && box.getYsize() <= ZoneSelectorItem.MAX && box.getZsize() <= ZoneSelectorItem.MAX;
        Vec3 cam = ctx.camera().getPosition();
        PoseStack pose = ctx.matrixStack();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        LevelRenderer.renderLineBox(pose, ctx.consumers().getBuffer(RenderType.lines()), box.inflate(0.002),
                ok ? 0.3F : 1F, ok ? 0.85F : 0.25F, ok ? 1F : 0.25F, 1F);
        pose.popPose();
    }
}
