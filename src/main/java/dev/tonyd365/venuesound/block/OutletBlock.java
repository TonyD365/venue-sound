package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Wall outlet / stage box: a passive patch point. Cables can run into it and on to further
 * outlets, a mixer, an amplifier or speakers, and one outlet can feed several cables (a splitter).
 */
public class OutletBlock extends DeviceBlock {

    private static final VoxelShape NORTH = box(3, 3, 14, 13, 13, 16);
    private static final VoxelShape SOUTH = box(3, 3, 0, 13, 13, 2);
    private static final VoxelShape EAST = box(0, 3, 3, 2, 13, 13);
    private static final VoxelShape WEST = box(14, 3, 3, 16, 13, 13);

    public OutletBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction facing = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SimpleNodeBlockEntity(pos, state);
    }
}
