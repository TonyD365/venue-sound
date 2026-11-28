package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.item.AudioCableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Line array module. Its rear panel has the INPUT socket, a power switch and a coverage knob. */
public class SpeakerBlock extends DeviceBlock {

    public SpeakerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SpeakerBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (player.getItemInHand(hand).getItem() instanceof AudioCableItem) {
            return InteractionResult.PASS;
        }
        if (useRearPanel(level, pos, state.getValue(FACING), player, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** Rear panel: power switch and coverage knob. Returns true if a control was hit. */
    static boolean useRearPanel(Level level, BlockPos device, Direction facing, Player player, BlockHitResult hit) {
        if (hit.getDirection() != facing.getOpposite() || !(level.getBlockEntity(device) instanceof SpeakerBlockEntity speaker)) {
            return false;
        }
        PanelMath.Point p = PanelMath.local(device, facing, hit.getLocation());
        PortLayout.SpeakerPanel panel = PortLayout.SpeakerPanel.of(speaker.deviceKey());
        if (panel.onSwitch(p.u(), p.height())) {
            if (!level.isClientSide && PanelInput.press(player, device, "power")) {
                speaker.togglePower();
                level.playSound(null, device, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, speaker.isOn() ? 0.9F : 0.7F);
                player.displayClientMessage(Component.translatable(speaker.isOn() ? "message.venuesound.speaker_on" : "message.venuesound.speaker_off"), true);
            }
            return true;
        }
        if (panel.onKnob(p.u(), p.height())) {
            if (!level.isClientSide) {
                speaker.stepRange(p.u() < panel.knobU());
                player.displayClientMessage(Component.translatable("message.venuesound.speaker_range", speaker.getRange()), true);
            }
            return true;
        }
        return false;
    }

}
