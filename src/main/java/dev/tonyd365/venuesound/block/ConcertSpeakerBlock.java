package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.item.AudioCableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Concert speaker stack: 4 blocks wide, 6 tall and 2 deep. Horn arrays on top, 15" mid-bass
 * drivers in the middle, 18" subwoofers at the bottom. The INPUT socket, power switch and
 * coverage knob are on the back, bottom-left as seen from the front.
 */
public class ConcertSpeakerBlock extends DeviceBlock {

    public static final int WIDTH = 4, HEIGHT = 6, DEEP = 2;
    public static final IntegerProperty COL = IntegerProperty.create("col", 0, WIDTH - 1);
    public static final IntegerProperty LVL = IntegerProperty.create("lvl", 0, HEIGHT - 1);
    public static final IntegerProperty DEP = IntegerProperty.create("dep", 0, DEEP - 1);

    public ConcertSpeakerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(COL, 0).setValue(LVL, 0).setValue(DEP, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COL, LVL, DEP);
    }

    private static BlockPos part(BlockPos main, Direction facing, int col, int lvl, int dep) {
        return main.relative(facing.getCounterClockWise(), col).above(lvl).relative(facing.getOpposite(), dep);
    }

    /** The front-left-bottom block (as seen from the front), which holds the block entity. */
    public static BlockPos mainPos(BlockState state, BlockPos pos) {
        Direction f = state.getValue(FACING);
        return pos.relative(f.getCounterClockWise(), -state.getValue(COL)).below(state.getValue(LVL)).relative(f.getOpposite(), -state.getValue(DEP));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        Direction f = state.getValue(FACING);
        Level level = context.getLevel();
        for (int c = 0; c < WIDTH; c++) {
            for (int l = 0; l < HEIGHT; l++) {
                for (int d = 0; d < DEEP; d++) {
                    BlockPos p = part(context.getClickedPos(), f, c, l, d);
                    if (p.getY() >= level.getMaxBuildHeight() || !level.getBlockState(p).canBeReplaced(context)) {
                        return null;
                    }
                }
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction f = state.getValue(FACING);
        for (int c = 0; c < WIDTH; c++) {
            for (int l = 0; l < HEIGHT; l++) {
                for (int d = 0; d < DEEP; d++) {
                    if (c != 0 || l != 0 || d != 0) {
                        level.setBlock(part(pos, f, c, l, d), state.setValue(COL, c).setValue(LVL, l).setValue(DEP, d), 3);
                    }
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockPos main = mainPos(state, pos);
            Direction f = state.getValue(FACING);
            for (int c = 0; c < WIDTH; c++) {
                for (int l = 0; l < HEIGHT; l++) {
                    for (int d = 0; d < DEEP; d++) {
                        BlockPos p = part(main, f, c, l, d);
                        if (!p.equals(pos) && level.getBlockState(p).is(this)) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                        }
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        boolean main = state.getValue(COL) == 0 && state.getValue(LVL) == 0 && state.getValue(DEP) == 0;
        return main ? new SpeakerBlockEntity(pos, state) : null;
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof AudioCableItem) {
            return InteractionResult.PASS;
        }
        if (SpeakerBlock.useRearPanel(level, mainPos(state, pos), state.getValue(FACING), player, hit)) {
            return dev.tonyd365.venuesound.compat.Use.sided(level);
        }
        return InteractionResult.PASS;
    }
}
