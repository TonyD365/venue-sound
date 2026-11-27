package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.item.AudioCableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Power amplifier, used on its front panel: the rocker switch is power, the big knob is level. */
public class AmplifierBlock extends DeviceBlock {

    public static final double SWITCH_U0 = PanelLayout.Amp.SWITCH_U0, SWITCH_U1 = PanelLayout.Amp.SWITCH_U1;
    public static final double SWITCH_H0 = PanelLayout.Amp.SWITCH_H0, SWITCH_H1 = PanelLayout.Amp.SWITCH_H1;
    public static final double KNOB_U = PanelLayout.Amp.KNOB_U, KNOB_H = PanelLayout.Amp.KNOB_H, KNOB_R = PanelLayout.Amp.KNOB_R;

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
        if (hit.getDirection() != state.getValue(FACING) || !(level.getBlockEntity(pos) instanceof AmplifierBlockEntity amp)) {
            return InteractionResult.PASS;
        }
        PanelMath.Point p = PanelMath.local(pos, state.getValue(FACING), hit.getLocation());
        if (!level.isClientSide) {
            if (PanelLayout.Amp.onSwitch(p.u(), p.height())) {
                if (PanelInput.press(player, pos, "power")) {
                    amp.togglePower();
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, amp.isOn() ? 0.9F : 0.7F);
                }
            } else if (PanelLayout.Amp.onKnob(p.u(), p.height())) {
                // holding right-click keeps turning the knob
                amp.cycleLevel(p.u() < KNOB_U);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
