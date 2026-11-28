package dev.tonyd365.venuesound.item;

import net.minecraft.ChatFormatting;
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
 * In-ear monitor earpiece: carried anywhere in the inventory, it plays the in-ear transmitter on
 * its channel. Right-click: next channel (sneak: previous). Give each performer their own
 * channel and transmitter for their own monitor mix.
 */
public class EarpieceItem extends Item {

    public static final int CHANNELS = 16;

    public EarpieceItem(Properties properties) {
        super(properties);
    }

    public static int getChannel(ItemStack stack) {
        int ch = stack.getTag() == null ? 1 : stack.getTag().getInt("Channel");
        return ch < 1 || ch > CHANNELS ? 1 : ch;
    }

    /** Whether the player carries an earpiece set to {@code channel}. */
    public static boolean carries(Player player, int channel) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof EarpieceItem && getChannel(s) == channel) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            int ch = Math.floorMod(getChannel(stack) - 1 + (player.isShiftKeyDown() ? -1 : 1), CHANNELS) + 1;
            stack.getOrCreateTag().putInt("Channel", ch);
            player.displayClientMessage(Component.translatable("message.venuesound.earpiece_channel", ch), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.venuesound.channel", getChannel(stack)).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.venuesound.earpiece_help").withStyle(ChatFormatting.GRAY));
    }
}
