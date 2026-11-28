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

/** Line array module or two-block PA cabinet: plays what reaches it through the cables. */
public class SpeakerBlockEntity extends AudioNodeBlockEntity {

    /** Coverage steps in blocks, cycled by right-clicking the speaker. */
    public static final int[] RANGES = {16, 32, 48, 64, 96, 128};

    private int range = 64;
    private boolean on = true;
    /** In-ear transmitter: which earpieces hear it (1-16). */
    private int channel = 1;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SPEAKER_BE, pos, state);
    }

    @Override
    public String deviceKey() {
        if (getBlockState().getBlock() instanceof ProSpeakerBlock p) {
            return p.key;
        }
        if (getBlockState().getBlock() instanceof ConcertSpeakerBlock) {
            return "concert_speaker";
        }
        return getBlockState().getBlock() instanceof PaSpeakerBlock ? "pa_speaker" : "speaker";
    }

    @Override
    public NodeKind kind() {
        return NodeKind.SPEAKER;
    }

    public boolean isOn() {
        return on;
    }

    public int getChannel() {
        return channel;
    }

    /** Next (or previous) channel, 1-16, wrapping round. */
    public void cycleChannel(boolean backwards) {
        channel = Math.floorMod(channel - 1 + (backwards ? -1 : 1), 16) + 1;
        changed();
    }

    public void togglePower() {
        on = !on;
        changed();
    }

    /** Turns the coverage knob one step (stops at the ends). */
    public void stepRange(boolean down) {
        int index = 0;
        for (int i = 0; i < RANGES.length; i++) {
            if (RANGES[i] == range) {
                index = i;
            }
        }
        int next = Math.max(0, Math.min(RANGES.length - 1, index + (down ? -1 : 1)));
        if (next != index) {
            range = RANGES[next];
            changed();
        }
    }

    public int getRange() {
        return range;
    }

    public void cycleRange(boolean backwards) {
        int index = 0;
        for (int i = 0; i < RANGES.length; i++) {
            if (RANGES[i] == range) {
                index = i;
            }
        }
        range = RANGES[Math.floorMod(index + (backwards ? -1 : 1), RANGES.length)];
        changed();
    }

    @Override
    protected RouterState.Node snapshot(ServerLevel level, List<RouterState.Out> outs) {
        double height = getBlockState().getBlock() instanceof ProSpeakerBlock p ? p.soundHeight
                : getBlockState().getBlock() instanceof ConcertSpeakerBlock ? 3.0D
                : getBlockState().getBlock() instanceof PaSpeakerBlock ? 1.0D : 0.5D;
        return new RouterState.Node(dimPos(), kind(), outs, channel, null, 0F, on ? range : 0, height);
    }

    @Override
    protected void write(CompoundTag tag) {
        super.write(tag);
        tag.putInt("Range", range);
        tag.putBoolean("On", on);
        tag.putInt("Channel", channel);
    }

    @Override
    protected void read(CompoundTag tag) {
        super.read(tag);
        range = tag.contains("Range") ? Nbt.getInt(tag, "Range") : 64;
        on = !tag.contains("On") || Nbt.getBoolean(tag, "On");
        channel = tag.contains("Channel") ? Math.max(1, Math.min(16, Nbt.getInt(tag, "Channel"))) : 1;
    }
}
