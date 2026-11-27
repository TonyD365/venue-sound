package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;

/** Data-driven speaker variant (floor wedge, subwoofer): same rear panel as the line array module. */
public class ProSpeakerBlock extends SpeakerBlock {

    public final String key;
    public final double soundHeight;
    private final VoxelShape shape;

    public ProSpeakerBlock(String key, double soundHeight, VoxelShape shape, Properties properties) {
        super(properties);
        this.key = key;
        this.soundHeight = soundHeight;
        this.shape = shape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
