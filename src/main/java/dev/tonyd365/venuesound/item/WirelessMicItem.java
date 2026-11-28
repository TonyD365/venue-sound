package dev.tonyd365.venuesound.item;

import dev.tonyd365.venuesound.block.ReceiverBlockEntity;
import net.minecraft.ChatFormatting;
import dev.tonyd365.venuesound.compat.Nbt;
import dev.tonyd365.venuesound.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Handheld wireless microphone. While it is held, the holder's voice is also transmitted on the
 * mic's channel to every receiver on that channel within range.
 */
public class WirelessMicItem extends dev.tonyd365.venuesound.compat.ModItem {

    public WirelessMicItem(Properties properties) {
        super(properties);
    }

    public static int getChannel(ItemStack stack) {
        CompoundTag tag = StackData.has(stack) ? StackData.read(stack) : null;
        int channel = tag == null ? 1 : Nbt.getInt(tag, "Channel");
        return channel < 1 || channel > ReceiverBlockEntity.CHANNELS ? 1 : channel;
    }

    public static boolean isMuted(ItemStack stack) {
        return Nbt.getBoolean(StackData.read(stack), "Muted");
    }

    /** Channel the player is currently transmitting on, or 0 if not holding an unmuted mic. */
    public static int activeChannel(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof WirelessMicItem && !isMuted(stack)) {
                return getChannel(stack);
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.getItem() instanceof BodyPackMicItem && !isMuted(stack)) {
                return getChannel(stack);
            }
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof BodyPackMicItem && !isMuted(stack)) {
                return getChannel(stack);
            }
        }
        return 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                boolean muted = !isMuted(stack);
                StackData.edit(stack, tag -> tag.putBoolean("Muted", muted));
                player.displayClientMessage(Component.translatable(muted ? "message.venuesound.mic_muted" : "message.venuesound.mic_live", getChannel(stack)), true);
            } else {
                nextChannel(stack, player);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Moves the mic to the next channel (16 wraps to 1) and shows it above the hotbar. */
    protected static void nextChannel(ItemStack stack, Player player) {
        int channel = getChannel(stack) % ReceiverBlockEntity.CHANNELS + 1;
        StackData.edit(stack, tag -> tag.putInt("Channel", channel));
        player.displayClientMessage(Component.translatable("message.venuesound.mic_channel", channel), true);
    }

    /** The last tooltip line: how to use this mic. */
    protected String helpKey() {
        return "tooltip.venuesound.mic_help";
    }

    @Override
    protected void addTooltip(ItemStack stack, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.venuesound.channel", getChannel(stack)).withStyle(ChatFormatting.AQUA));
        tooltip.add(isMuted(stack)
                ? Component.translatable("tooltip.venuesound.muted").withStyle(ChatFormatting.RED)
                : Component.translatable("tooltip.venuesound.live").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable(helpKey()).withStyle(ChatFormatting.GRAY));
    }
}
