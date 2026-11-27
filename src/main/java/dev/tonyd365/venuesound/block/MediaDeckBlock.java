package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.item.AudioCableItem;
import dev.tonyd365.venuesound.net.DeckPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

/**
 * Media deck, used on its front panel: previous, play, stop, next and loop buttons under a
 * display showing the track. Sneak + right-click (empty hand) opens the track list.
 */
public class MediaDeckBlock extends DeviceBlock {

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 8, 16);

    /** Button row on the front face, left to right (panel units). */
    public static final String[] BUTTONS = PanelLayout.Deck.BUTTONS;
    public static final double BUTTON_H0 = PanelLayout.Deck.BUTTON_H0, BUTTON_H1 = PanelLayout.Deck.BUTTON_H1;

    public static double buttonU0(int i) {
        return PanelLayout.Deck.buttonU0(i);
    }

    public MediaDeckBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MediaDeckBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof AudioCableItem) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof MediaDeckBlockEntity deck)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer sp) {
                DeckPackets.openScreen(sp, deck);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() != state.getValue(FACING)) {
            return InteractionResult.PASS;
        }
        PanelMath.Point p = PanelMath.local(pos, state.getValue(FACING), hit.getLocation());
        String button = PanelLayout.Deck.buttonAt(p.u(), p.height());
        if (!level.isClientSide && button != null && PanelInput.press(player, pos, button)) {
            deck.press(button);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.3F, 1.8F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
