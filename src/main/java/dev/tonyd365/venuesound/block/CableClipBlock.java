package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cable clip: fixes cables to a wall, floor or ceiling. While holding a plugged-in cable,
 * right-click clips one after another to run the cable along them before plugging it in.
 */
public class CableClipBlock extends DirectionalBlock {

    public CableClipBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case UP -> box(5, 0, 5, 11, 3, 11);
            case DOWN -> box(5, 13, 5, 11, 16, 11);
            case NORTH -> box(5, 5, 13, 11, 11, 16);
            case SOUTH -> box(5, 5, 0, 11, 11, 3);
            case EAST -> box(0, 5, 5, 3, 11, 11);
            case WEST -> box(13, 5, 5, 16, 11, 11);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** Where a cable passes through the clip (close to the surface it is fixed to). */
    public static Vec3 anchor(BlockState state, BlockPos pos) {
        Direction f = state.getValue(FACING);
        return Vec3.atCenterOf(pos).subtract(f.getStepX() * 0.36, f.getStepY() * 0.36, f.getStepZ() * 0.36);
    }
}
