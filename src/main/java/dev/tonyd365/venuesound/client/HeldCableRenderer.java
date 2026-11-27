package dev.tonyd365.venuesound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
import dev.tonyd365.venuesound.block.CableClipBlock;
import dev.tonyd365.venuesound.block.PortLayout;
import dev.tonyd365.venuesound.item.AudioCableItem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * While you hold a cable that is plugged in at one end, draws it from that socket, through the
 * clips you have run it along, to your hand.
 */
public final class HeldCableRenderer {

    private HeldCableRenderer() {
    }

    public static void render(WorldRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) {
            return;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            CompoundTag tag = stack.getTag();
            if (!(stack.getItem() instanceof AudioCableItem) || tag == null || !tag.contains("From")
                    || !level.dimension().location().toString().equals(tag.getString("FromDim"))) {
                continue;
            }
            BlockPos from = BlockPos.of(tag.getLong("From"));
            if (!(level.getBlockEntity(from) instanceof AudioNodeBlockEntity src)) {
                continue;
            }
            List<Vec3> points = new ArrayList<>();
            points.add(AudioCableItem.socketWorld(level, from, src.deviceKey(), PortLayout.Dir.OUT, tag.getInt("FromPort")));
            for (long v : tag.getLongArray("Via")) {
                BlockPos clip = BlockPos.of(v);
                BlockState cs = level.getBlockState(clip);
                if (cs.getBlock() instanceof CableClipBlock) {
                    points.add(CableClipBlock.anchor(cs, clip));
                }
            }
            points.add(handPosition(player, hand, ctx.tickDelta()));
            PoseStack pose = ctx.matrixStack();
            Vec3 cam = ctx.camera().getPosition();
            pose.pushPose();
            pose.translate(-cam.x, -cam.y, -cam.z);
            MultiBufferSource buffers = ctx.consumers();
            if (buffers != null) {
                VertexConsumer vc = buffers.getBuffer(RenderType.debugQuads());
                CableRenderer.drawRun(level, vc, pose.last().pose(), points, Vec3.ZERO);
            }
            pose.popPose();
        }
    }

    /** Roughly where the held item is: in front of the body, to the side of the hand holding it. */
    private static Vec3 handPosition(LocalPlayer player, InteractionHand hand, float partial) {
        Vec3 eye = player.getEyePosition(partial);
        float yaw = (float) Math.toRadians(player.getViewYRot(partial));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 right = new Vec3(-forward.z, 0, forward.x);
        boolean rightHand = (hand == InteractionHand.MAIN_HAND) == (player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT);
        return eye.add(forward.scale(0.45)).add(right.scale(rightHand ? 0.35 : -0.35)).subtract(0, 0.55, 0);
    }
}
