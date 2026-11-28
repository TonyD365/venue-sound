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
        if (getBlockState().getBlock() instanceof StandMicBlock m) {
            return m.key;
        }
        return getBlockState().getBlock() instanceof StageBoxBlock b ? (b.channels == 16 ? "stage_box_16" : "stage_box") : "outlet";
    }

    @Override
    protected dev.tonyd365.venuesound.audio.RouterState.Node snapshot(net.minecraft.server.level.ServerLevel level, java.util.List<dev.tonyd365.venuesound.audio.RouterState.Out> outs) {
        int range = getBlockState().getBlock() instanceof StandMicBlock m ? m.range : 0;
        // channel flags: a stage box is multichannel; a condenser mic (gooseneck, choir) needs +48V
        int channel = getBlockState().getBlock() instanceof StageBoxBlock ? 1
                : getBlockState().getBlock() instanceof StandMicBlock && !"stand_mic".equals(deviceKey()) ? 1 : 0;
        return new dev.tonyd365.venuesound.audio.RouterState.Node(dimPos(), kind(), outs, channel, null, 0F, range, 0.5D);
    }

    @Override
    public NodeKind kind() {
        return getBlockState().getBlock() instanceof StandMicBlock ? NodeKind.STAND_MIC : NodeKind.OUTLET;
    }
}
