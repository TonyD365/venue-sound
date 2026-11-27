package dev.tonyd365.venuesound.item;

import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.Patchable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Patch cable: click a receiver or speaker, then a mixer, to connect them.
 * Sneak-click a device to unplug it; sneak-click the air to drop the held end.
 */
public class AudioCableItem extends Item {

    public static final int MAX_LENGTH = 64;

    public AudioCableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        BlockEntity target = level.getBlockEntity(clicked);

        if (target instanceof Patchable device) {
            if (player.isShiftKeyDown()) {
                device.setMixer(null);
                player.displayClientMessage(Component.translatable("message.venuesound.cable_unplugged"), true);
            } else {
                CompoundTag tag = stack.getOrCreateTag();
                tag.put("Source", NbtUtils.writeBlockPos(clicked));
                tag.putString("SourceDim", level.dimension().location().toString());
                player.displayClientMessage(Component.translatable("message.venuesound.cable_source"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (target instanceof MixerBlockEntity) {
            CompoundTag tag = stack.getTag();
            if (tag == null || !tag.contains("Source")) {
                player.displayClientMessage(Component.translatable("message.venuesound.cable_need_source").withStyle(ChatFormatting.YELLOW), true);
                return InteractionResult.SUCCESS;
            }
            BlockPos source = NbtUtils.readBlockPos(tag.getCompound("Source"));
            boolean sameDim = level.dimension().location().toString().equals(tag.getString("SourceDim"));
            if (!sameDim || !source.closerThan(clicked, MAX_LENGTH)) {
                player.displayClientMessage(Component.translatable("message.venuesound.cable_too_far", MAX_LENGTH).withStyle(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(source) instanceof Patchable device) {
                device.setMixer(clicked);
                player.displayClientMessage(Component.translatable("message.venuesound.cable_connected").withStyle(ChatFormatting.GREEN), true);
            } else {
                player.displayClientMessage(Component.translatable("message.venuesound.cable_source_gone").withStyle(ChatFormatting.RED), true);
            }
            tag.remove("Source");
            tag.remove("SourceDim");
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.getTag() != null && stack.getTag().contains("Source")) {
            if (!level.isClientSide) {
                stack.getTag().remove("Source");
                stack.getTag().remove("SourceDim");
                player.displayClientMessage(Component.translatable("message.venuesound.cable_cleared"), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains("Source");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.venuesound.cable_help").withStyle(ChatFormatting.GRAY));
    }
}
