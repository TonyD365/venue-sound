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
 * Intercom beltpack with headset, for the crew. Carried anywhere in the inventory, it hears
 * everyone talking on its channel (A or B) and the base station's program feed; with TALK on,
 * your voice goes to everyone on your channel near a powered base station. The audience never
 * hears it. Right-click: switch A / B. Sneak + right-click: TALK on / off.
 */
public class IntercomBeltpackItem extends Item {

    public IntercomBeltpackItem(Properties properties) {
        super(properties);
    }

    /** 1 = A, 2 = B. */
    public static int getChannel(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getInt("Channel") == 2 ? 2 : 1;
    }

    public static boolean isTalking(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().getBoolean("Talk");
    }

    public static String channelName(int channel) {
        return channel == 2 ? "B" : "A";
    }

    /** The first beltpack the player carries, or null. */
    @Nullable
    public static ItemStack carried(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof IntercomBeltpackItem) {
                return s;
            }
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var tag = stack.getOrCreateTag();
            if (player.isShiftKeyDown()) {
                boolean talk = !isTalking(stack);
                tag.putBoolean("Talk", talk);
                player.displayClientMessage(Component.translatable(talk ? "message.venuesound.intercom_talk_on" : "message.venuesound.intercom_talk_off",
                        channelName(getChannel(stack))).withStyle(talk ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
            } else {
                int ch = getChannel(stack) == 1 ? 2 : 1;
                tag.putInt("Channel", ch);
                player.displayClientMessage(Component.translatable("message.venuesound.intercom_channel", channelName(ch)), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.venuesound.intercom_channel", channelName(getChannel(stack))).withStyle(ChatFormatting.AQUA));
        tooltip.add(isTalking(stack)
                ? Component.translatable("tooltip.venuesound.intercom_talk_on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("tooltip.venuesound.intercom_talk_off").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.venuesound.intercom_help").withStyle(ChatFormatting.GRAY));
    }
}
