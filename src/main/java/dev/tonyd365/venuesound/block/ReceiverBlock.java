package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.item.AudioCableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Wireless receiver, used on its front panel: the display shows the channel, two buttons change it. */
public class ReceiverBlock extends DeviceBlock {

    private static final VoxelShape SHAPE = box(2, 0, 3, 14, 5, 13);

    /** Front panel (panel units). The front face sits 3 units in from the block edge. */
    public static final double FRONT_DEPTH = PanelLayout.Receiver.FRONT_DEPTH;
    public static final double DOWN_U0 = PanelLayout.Receiver.DOWN_U0, UP_U0 = PanelLayout.Receiver.UP_U0;
    public static final double BUTTON_W = PanelLayout.Receiver.BUTTON_W;
    public static final double BUTTON_H0 = PanelLayout.Receiver.BUTTON_H0, BUTTON_H1 = PanelLayout.Receiver.BUTTON_H1;

    public ReceiverBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReceiverBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof AudioCableItem) {
            return InteractionResult.PASS;
        }
        if (hit.getDirection() != state.getValue(FACING) || !(level.getBlockEntity(pos) instanceof ReceiverBlockEntity receiver)) {
            return InteractionResult.PASS;
        }
        PanelMath.Point p = PanelMath.local(pos, state.getValue(FACING), hit.getLocation());
        int button = PanelLayout.Receiver.buttonAt(p.u(), p.height());
        if (!level.isClientSide && button != 0 && PanelInput.press(player, pos, button > 0 ? "up" : "down")) {
            receiver.cycleChannel(button < 0);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.8F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
