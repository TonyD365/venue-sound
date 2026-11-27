package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** A device that can be patched to a mixer with the audio cable. */
public interface Patchable {

    void setMixer(@Nullable BlockPos mixer);

    @Nullable
    BlockPos getMixer();
}
