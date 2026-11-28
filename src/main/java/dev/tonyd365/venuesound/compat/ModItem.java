package dev.tonyd365.venuesound.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** An item with tooltip lines, on every Minecraft version (the tooltip method changed in 1.20.5). */
public abstract class ModItem extends Item {

    protected ModItem(Properties properties) {
        super(properties);
    }

    /** Adds this stack's tooltip lines under its name. */
    protected abstract void addTooltip(ItemStack stack, List<Component> tooltip);

    //? if >=1.20.5 {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        addTooltip(stack, tooltip);
    }
    *///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        addTooltip(stack, tooltip);
    }
    //?}
}
