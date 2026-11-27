package dev.tonyd365.venuesound.block;

import net.minecraft.util.StringRepresentable;

/** The two halves of the console, as seen by the operator. */
public enum MixerPart implements StringRepresentable {
    LEFT, RIGHT;

    @Override
    public String getSerializedName() {
        return this == LEFT ? "left" : "right";
    }
}
