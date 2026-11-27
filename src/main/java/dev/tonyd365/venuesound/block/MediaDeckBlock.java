package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.item.AudioCableItem;
import dev.tonyd365.venuesound.net.DeckPackets;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

/** Media deck: right-click to pick a track and play it into the cables. */
public class MediaDeckBlock extends DeviceBlock {

    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 8, 16);

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
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof MediaDeckBlockEntity deck) {
            DeckPackets.openScreen(sp, deck);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
