package dev.tonyd365.venuesound.item;

import dev.tonyd365.venuesound.block.VideoCaptureBlockEntity;
import dev.tonyd365.venuesound.compat.Nbt;
import dev.tonyd365.venuesound.compat.StackData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Zone Selector: click two corners (max 48 blocks per axis), then click a Video Capture block to
 * give it that zone. Sneak-use in the air clears the selection.
 */
public class ZoneSelectorItem extends Item {

    public static final int MAX = 48;

    public ZoneSelectorItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static BlockPos corner(ItemStack stack, String key) {
        CompoundTag t = StackData.read(stack);
        return t.contains(key) ? BlockPos.of(Nbt.getLong(t, key)) : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        ItemStack stack = ctx.getItemInHand();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos a = corner(stack, "A"), b = corner(stack, "B");
        if (level.getBlockEntity(pos) instanceof VideoCaptureBlockEntity cap) {
            if (a != null && b != null) {
                cap.setZone(a, b);
                msg(player, "message.venuesound.zone_set");
            } else {
                msg(player, "message.venuesound.zone_none");
            }
            return InteractionResult.SUCCESS;
        }
        if (a == null || b != null) {
            StackData.edit(stack, tag -> {
                tag.putLong("A", pos.asLong());
                tag.remove("B");
            });
            msg(player, "message.venuesound.zone_first");
        } else if (Math.abs(a.getX() - pos.getX()) >= MAX || Math.abs(a.getY() - pos.getY()) >= MAX || Math.abs(a.getZ() - pos.getZ()) >= MAX) {
            msg(player, "message.venuesound.zone_too_big");
        } else {
            StackData.edit(stack, tag -> tag.putLong("B", pos.asLong()));
            msg(player, "message.venuesound.zone_second");
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && StackData.has(stack)) {
            StackData.clear(stack);
            if (!level.isClientSide) {
                msg(player, "message.venuesound.zone_cleared");
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    private static void msg(@Nullable Player p, String key) {
        if (p != null) {
            p.displayClientMessage(Component.translatable(key), true);
        }
    }
}
