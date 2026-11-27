package dev.tonyd365.venuesound.item;

import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.PaSpeakerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Audio cable. Click the device the signal comes FROM, then the device it goes TO.
 * Sneak-click a device to unplug every cable it sends out (on a mixer: also its inputs).
 * Sneak-click the air to drop the held end.
 */
public class AudioCableItem extends Item {

    /** Longest single cable run, in blocks. Use outlets to go further. */
    public static final int MAX_LENGTH = 32;

    public AudioCableItem(Properties properties) {
        super(properties);
    }

    /** Resolves multi-block devices (the PA cabinet's top half) to the block holding the device. */
    public static BlockPos devicePos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof PaSpeakerBlock ? PaSpeakerBlock.basePos(state, pos) : pos;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clicked = devicePos(level, context.getClickedPos());
        if (!(level.getBlockEntity(clicked) instanceof AudioNodeBlockEntity target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();

        if (player.isShiftKeyDown()) {
            target.clearOutputs();
            if (target instanceof MixerBlockEntity mixer) {
                mixer.clearInputs();
            }
            clearHeld(stack);
            say(player, Component.translatable("message.venuesound.cable_unplugged"));
            return InteractionResult.SUCCESS;
        }

        BlockPos from = heldFrom(stack, level);
        if (from == null) {
            // First click: pick up the cable end at the device the signal comes from.
            if (target.kind().maxOutputs() == 0) {
                say(player, Component.translatable("message.venuesound.cable_no_output").withStyle(ChatFormatting.YELLOW));
            } else if (!target.canAddOutput()) {
                say(player, Component.translatable("message.venuesound.cable_outputs_full").withStyle(ChatFormatting.YELLOW));
            } else {
                CompoundTag tag = stack.getOrCreateTag();
                tag.putLong("From", clicked.asLong());
                tag.putString("FromDim", level.dimension().location().toString());
                say(player, Component.translatable("message.venuesound.cable_from"));
            }
            return InteractionResult.SUCCESS;
        }

        // Second click: plug the other end into this device.
        clearHeld(stack);
        if (from.equals(clicked)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(from) instanceof AudioNodeBlockEntity source)) {
            say(player, Component.translatable("message.venuesound.cable_source_gone").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (!from.closerThan(clicked, MAX_LENGTH)) {
            say(player, Component.translatable("message.venuesound.cable_too_far", MAX_LENGTH).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (!source.kind().canFeed(target.kind())) {
            say(player, Component.translatable("message.venuesound.cable_wrong_way").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (source.hasOutput(clicked)) {
            say(player, Component.translatable("message.venuesound.cable_already").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }
        if (!source.canAddOutput()) {
            say(player, Component.translatable("message.venuesound.cable_outputs_full").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }
        if (target instanceof MixerBlockEntity mixer) {
            int input = mixer.assignInput(from);
            if (input < 0) {
                say(player, Component.translatable("message.venuesound.cable_mixer_full").withStyle(ChatFormatting.RED));
                return InteractionResult.SUCCESS;
            }
            source.addOutput(clicked);
            say(player, Component.translatable("message.venuesound.cable_mixer_input", input).withStyle(ChatFormatting.GREEN));
            return InteractionResult.SUCCESS;
        }
        source.addOutput(clicked);
        say(player, Component.translatable("message.venuesound.cable_connected").withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }

    @Nullable
    private static BlockPos heldFrom(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("From")) {
            return null;
        }
        if (!level.dimension().location().toString().equals(tag.getString("FromDim"))) {
            clearHeld(stack);
            return null;
        }
        return BlockPos.of(tag.getLong("From"));
    }

    private static void clearHeld(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove("From");
            tag.remove("FromDim");
        }
    }

    private static void say(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.getTag() != null && stack.getTag().contains("From")) {
            if (!level.isClientSide) {
                clearHeld(stack);
                say(player, Component.translatable("message.venuesound.cable_cleared"));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains("From");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.venuesound.cable_help").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.venuesound.cable_help2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
