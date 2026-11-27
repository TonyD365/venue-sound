package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Generic in-line processor (graphic EQ, compressor, DI box...): one data-driven class, keyed by
 * block id. Front panel works like the amplifier: power rocker and level knob; it sits in the
 * chain as {@link dev.tonyd365.venuesound.audio.NodeKind#PROCESSOR}.
 */
public class ProcessorBlock extends AmplifierBlock {

    public final String key;

    public ProcessorBlock(String key, Properties properties) {
        super(properties);
        this.key = key;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AmplifierBlockEntity(pos, state);
    }
}
