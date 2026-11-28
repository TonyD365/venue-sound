package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.DimPos;
import dev.tonyd365.venuesound.audio.NodeKind;
import dev.tonyd365.venuesound.audio.RouterState;
import net.minecraft.core.BlockPos;
import dev.tonyd365.venuesound.compat.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Wireless receiver: picks up wireless mics on its channel. */
public class ReceiverBlockEntity extends AudioNodeBlockEntity {

    public static final int CHANNELS = 16;

    private int channel = 1;

    public ReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.RECEIVER_BE, pos, state);
    }

    @Override
    public String deviceKey() {
        return "receiver";
    }

    @Override
    public NodeKind kind() {
        return NodeKind.RECEIVER;
    }

    public int getChannel() {
        return channel;
    }

    public void cycleChannel(boolean backwards) {
        channel = Math.floorMod(channel - 1 + (backwards ? -1 : 1), CHANNELS) + 1;
        changed();
    }

    @Override
    protected RouterState.Node snapshot(ServerLevel level, List<RouterState.Out> outs) {
        return new RouterState.Node(dimPos(), kind(), outs, channel, null, 0F, 0, 0.5D);
    }

    @Override
    protected void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("Channel", channel);
    }

    @Override
    protected void read(CompoundTag tag) {
        super.read(tag);
        channel = Math.max(1, Math.min(CHANNELS, Nbt.getInt(tag, "Channel")));
    }
}
