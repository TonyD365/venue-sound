package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.item.AudioCableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Power amplifier. Right-click: output level. Sneak + right-click (empty hand): power. */
public class AmplifierBlock extends DeviceBlock {

    public AmplifierBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AmplifierBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof AudioCableItem) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AmplifierBlockEntity amp) {
            if (player.isShiftKeyDown()) {
                amp.togglePower();
            } else {
                amp.cycleLevel(false);
            }
            player.displayClientMessage(amp.isOn()
                    ? Component.translatable("message.venuesound.amp_on", amp.getLevelPercent())
                    : Component.translatable("message.venuesound.amp_off"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
