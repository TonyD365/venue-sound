package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Power sequencer: right-click the front to switch every amplifier and speaker within 16 blocks.
 * On: speakers first, amplifiers last; off: amplifiers first. Steps run 4 ticks apart.
 */
public class SequencerBlock extends RackBlock {

    public static final int RADIUS = 16;

    public SequencerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!PanelInput.press(player, pos, "power")) {
            return InteractionResult.SUCCESS;
        }
        List<BlockEntity> amps = new ArrayList<>(), speakers = new ArrayList<>();
        boolean anyOn = false;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -RADIUS, -RADIUS), pos.offset(RADIUS, RADIUS, RADIUS))) {
            BlockEntity be = level.getBlockEntity(p);
            if (be instanceof AmplifierBlockEntity a && !(a.getBlockState().getBlock() instanceof ProcessorBlock)) {
                amps.add(a);
                anyOn |= a.isOn();
            } else if (be instanceof SpeakerBlockEntity s) {
                speakers.add(s);
            }
        }
        boolean target = !anyOn;
        Comparator<BlockEntity> byDist = Comparator.comparingDouble(b -> b.getBlockPos().distSqr(pos));
        amps.sort(byDist);
        speakers.sort(byDist);
        List<BlockEntity> order = new ArrayList<>(target ? speakers : amps);
        order.addAll(target ? amps : speakers);
        int delay = 0;
        for (BlockEntity be : order) {
            int d = delay;
            delay += 4;
            level.getServer().execute(new net.minecraft.server.TickTask(level.getServer().getTickCount() + d, () -> {
                if (be instanceof AmplifierBlockEntity a && a.isOn() != target) {
                    a.togglePower();
                } else if (be instanceof SpeakerBlockEntity s && s.isOn() != target) {
                    s.togglePower();
                }
                level.playSound(null, be.getBlockPos(), SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, target ? 0.9F : 0.7F);
            }));
        }
        player.displayClientMessage(Component.translatable(target ? "message.venuesound.sequencer_on" : "message.venuesound.sequencer_off", order.size()), true);
        return InteractionResult.SUCCESS;
    }
}
