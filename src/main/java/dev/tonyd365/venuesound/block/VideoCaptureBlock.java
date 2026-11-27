package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Video Capture: a source with an OUT socket; screens inside its zone play through the console. */
public class VideoCaptureBlock extends DeviceBlock {
    public VideoCaptureBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VideoCaptureBlockEntity(pos, state);
    }
}
