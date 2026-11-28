package dev.tonyd365.venuesound.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * The settings an item keeps (mic channel, cable ends, zone corners): the stack's NBT tag on
 * 1.20.1, the minecraft:custom_data component since 1.20.5.
 */
public final class StackData {

    private StackData() {
    }

    /** Whether the stack has any data. */
    public static boolean has(ItemStack stack) {
        //? if >=1.20.5 {
        /*return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        *///?} else
        return stack.hasTag();
    }

    /** The data to read from (empty if none). Changing it does not change the stack: use {@link #edit}. */
    public static CompoundTag read(ItemStack stack) {
        //? if >=1.20.5 {
        /*return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        *///?} else {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag;
        //?}
    }

    /** Changes the stack's data (created if missing). */
    public static void edit(ItemStack stack, Consumer<CompoundTag> change) {
        //? if >=1.20.5 {
        /*net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, change);
        *///?} else
        change.accept(stack.getOrCreateTag());
    }

    /** Removes all of the stack's data. */
    public static void clear(ItemStack stack) {
        //? if >=1.20.5 {
        /*stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        *///?} else
        stack.setTag(null);
    }
}
