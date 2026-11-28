package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.item.AudioCableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The mixing console: three blocks wide and two deep. Every control on its top is used by
 * right-clicking it (hold right-click on a fader and move the crosshair to drag it; click the
 * left or right half of a knob to turn it). The input and output sockets are on the front side.
 */
public class MixerBlock extends DeviceBlock {

    public static final int COLS = 3;
    public static final int ROWS = 2;
    /** Column from the operator's left (0-2) and row from the front (0-1). */
    public static final IntegerProperty COL = IntegerProperty.create("col", 0, COLS - 1);
    public static final IntegerProperty ROW = IntegerProperty.create("row", 0, ROWS - 1);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 8, 16);

    public MixerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(colProperty(), 0).setValue(ROW, 0));
    }

    /** Which console this is (the VS-24 overrides). */
    public MixerLayout layout() {
        return MixerLayout.VS12;
    }

    protected IntegerProperty colProperty() {
        return COL;
    }

    /** Column of any console part, whichever console it belongs to. */
    public static int col(BlockState state) {
        return state.hasProperty(COL) ? state.getValue(COL) : state.getValue(MixerBlock24.COL5);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(colProperty(), ROW);
    }

    private static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** The front-left block, which holds the block entity. */
    public static BlockPos mainPos(BlockState state, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return pos.relative(right(facing), -col(state)).relative(facing.getOpposite(), -state.getValue(ROW));
    }

    private static BlockPos partPos(BlockPos main, Direction facing, int col, int row) {
        return main.relative(right(facing), col).relative(facing.getOpposite(), row);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context).setValue(colProperty(), 0).setValue(ROW, 0);
        Direction facing = state.getValue(FACING);
        for (int c = 0; c < layout().cols; c++) {
            for (int r = 0; r < ROWS; r++) {
                BlockPos p = partPos(context.getClickedPos(), facing, c, r);
                if (!context.getLevel().getBlockState(p).canBeReplaced(context) || !context.getLevel().getWorldBorder().isWithinBounds(p)) {
                    return null;
                }
            }
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction facing = state.getValue(FACING);
        for (int c = 0; c < layout().cols; c++) {
            for (int r = 0; r < ROWS; r++) {
                if (c != 0 || r != 0) {
                    level.setBlock(partPos(pos, facing, c, r), state.setValue(colProperty(), c).setValue(ROW, r), 3);
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            // breaking any part removes the whole console (only the main part drops it)
            BlockPos main = mainPos(state, pos);
            Direction facing = state.getValue(FACING);
            for (int c = 0; c < layout().cols; c++) {
                for (int r = 0; r < ROWS; r++) {
                    BlockPos p = partPos(main, facing, c, r);
                    if (!p.equals(pos) && level.getBlockState(p).is(this)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return col(state) == 0 && state.getValue(ROW) == 0 ? new MixerBlockEntity(pos, state) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof AudioCableItem || hit.getDirection() != Direction.UP) {
            return InteractionResult.PASS;
        }
        BlockPos main = mainPos(state, pos);
        if (!(level.getBlockEntity(main) instanceof MixerBlockEntity mixer)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            OscTarget.touched(main); // an OSC remote follows the console you last used
        }
        PanelMath.Point p = PanelMath.local(main, state.getValue(FACING), hit.getLocation());
        MixerLayout.Control c = layout().at(p.u(), p.depth());
        if (c == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            switch (c.kind()) {
                case FADER, KNOB -> mixer.set(c.param(), MixerLayout.apply(c, mixer.get(c.param()), p.u(), p.depth()));
                case BUTTON -> {
                    if (PanelInput.press(player, main, c.param())) {
                        if (c.param().startsWith("usb.")) {
                            mixer.usbButton(c.param().substring(4));
                        } else {
                            mixer.set(c.param(), MixerLayout.apply(c, mixer.get(c.param()), p.u(), p.depth()));
                        }
                        level.playSound(null, main, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.7F);
                    }
                }
                default -> {
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
