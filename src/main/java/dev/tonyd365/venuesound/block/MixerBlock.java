package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.item.AudioCableItem;
import dev.tonyd365.venuesound.net.MixerPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Two-block mixing console. Every control is used directly on the model: right-click a fader
 * to set it and hold right-click while moving the crosshair to drag it; click a mute button to
 * toggle it; click the left or right half of a trim knob to turn it. Sneak + right-click with
 * an empty hand opens the console screen.
 */
public class MixerBlock extends DeviceBlock {

    public static final EnumProperty<MixerPart> PART = EnumProperty.create("part", MixerPart.class);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 10, 16);

    public MixerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, MixerPart.LEFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    /** Direction from the left half to the right half. */
    private static Direction toRight(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    /** The left half, which holds the block entity. */
    public static BlockPos mainPos(BlockState state, BlockPos pos) {
        return state.getValue(PART) == MixerPart.RIGHT ? pos.relative(toRight(state).getOpposite()) : pos;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context).setValue(PART, MixerPart.LEFT);
        BlockPos right = context.getClickedPos().relative(toRight(state));
        return context.getLevel().getBlockState(right).canBeReplaced(context) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.relative(toRight(state)), state.setValue(PART, MixerPart.RIGHT), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        Direction toOther = state.getValue(PART) == MixerPart.LEFT ? toRight(state) : toRight(state).getOpposite();
        if (direction == toOther && !(neighbor.is(this) && neighbor.getValue(PART) != state.getValue(PART))) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) == MixerPart.RIGHT) {
            BlockPos main = mainPos(state, pos);
            if (level.getBlockState(main).is(this)) {
                level.setBlock(main, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == MixerPart.LEFT ? new MixerBlockEntity(pos, state) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof AudioCableItem) {
            return InteractionResult.PASS;
        }
        BlockPos main = mainPos(state, pos);
        if (!(level.getBlockEntity(main) instanceof MixerBlockEntity mixer)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer sp) {
                MixerPackets.openScreen(sp, mixer);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() != Direction.UP) {
            return InteractionResult.PASS;
        }
        PanelMath.Point p = PanelMath.local(main, state.getValue(FACING), hit.getLocation());
        if (!level.isClientSide) {
            handleClick(level, main, player, mixer, p);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void handleClick(Level level, BlockPos pos, Player player, MixerBlockEntity mixer, PanelMath.Point p) {
        int strip = MixerLayout.stripAt(p.u());
        if (strip < 0 || p.depth() >= MixerLayout.BACK_DEPTH) {
            return;
        }
        double depth = p.depth();
        if (depth >= MixerLayout.FADER_MIN - 0.6 && depth <= MixerLayout.FADER_MAX + 0.6) {
            double v = MixerLayout.faderValue(depth);
            if (Math.abs(v - 0.5) < 0.04) {
                v = 0.5; // snap to unity gain, like the 0 dB mark
            }
            float gain = (float) (v * MixerBlockEntity.MAX_GAIN);
            if (strip == 8) {
                mixer.apply(MixerBlockEntity.INPUTS, gain, false);
            } else {
                mixer.apply(strip, gain, mixer.isMuted(strip));
            }
            return;
        }
        if (strip == 8) {
            return;
        }
        double u = MixerLayout.stripU(strip);
        if (depth >= MixerLayout.MUTE_MIN && depth <= MixerLayout.MUTE_MAX) {
            if (PanelInput.press(player, pos, "mute" + strip)) {
                mixer.apply(strip, mixer.getGain(strip), !mixer.isMuted(strip));
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.6F);
            }
            return;
        }
        if (Math.abs(depth - MixerLayout.KNOB_DEPTH) <= MixerLayout.KNOB_RADIUS + 0.3) {
            mixer.stepTrim(strip, p.u() < u ? -1 : 1);
        }
    }
}
