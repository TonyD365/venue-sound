package dev.tonyd365.venuesound.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads NBT: a missing or mistyped key gives 0, false, "" or an empty value. All NBT reads go
 * through here so a later Minecraft (whose CompoundTag getters return Optionals) needs one change.
 */
public final class Nbt {

    private Nbt() {
    }

    public static int getInt(CompoundTag tag, String key) {
        return tag.getInt(key);
    }

    public static long getLong(CompoundTag tag, String key) {
        return tag.getLong(key);
    }

    public static float getFloat(CompoundTag tag, String key) {
        return tag.getFloat(key);
    }

    public static boolean getBoolean(CompoundTag tag, String key) {
        return tag.getBoolean(key);
    }

    public static String getString(CompoundTag tag, String key) {
        return tag.getString(key);
    }

    public static long[] getLongArray(CompoundTag tag, String key) {
        return tag.getLongArray(key);
    }

    public static CompoundTag getCompound(CompoundTag tag, String key) {
        return tag.getCompound(key);
    }

    public static Set<String> keys(CompoundTag tag) {
        return tag.getAllKeys();
    }

    private static ListTag list(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LIST) ? (ListTag) tag.get(key) : new ListTag();
    }

    /** The compound entries of the list at {@code key}. */
    public static List<CompoundTag> compounds(CompoundTag tag, String key) {
        List<CompoundTag> out = new ArrayList<>();
        for (Tag t : list(tag, key)) {
            if (t instanceof CompoundTag c) {
                out.add(c);
            }
        }
        return out;
    }

    /** The long entries of the list at {@code key}. */
    public static List<Long> longs(CompoundTag tag, String key) {
        List<Long> out = new ArrayList<>();
        for (Tag t : list(tag, key)) {
            if (t instanceof net.minecraft.nbt.LongTag l) {
                out.add(l.getAsLong());
            }
        }
        return out;
    }
}
