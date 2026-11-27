package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.NodeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Devices with no settings of their own: wall outlets and stand mics. */
public class SimpleNodeBlockEntity extends AudioNodeBlockEntity {

    public SimpleNodeBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SIMPLE_NODE_BE, pos, state);
    }

    @Override
    public String deviceKey() {
        return getBlockState().getBlock() instanceof StandMicBlock ? "stand_mic" : "outlet";
    }

    @Override
    public NodeKind kind() {
        return getBlockState().getBlock() instanceof StandMicBlock ? NodeKind.STAND_MIC : NodeKind.OUTLET;
    }
}
