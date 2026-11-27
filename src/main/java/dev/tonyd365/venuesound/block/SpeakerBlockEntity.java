package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.DimPos;
import dev.tonyd365.venuesound.audio.NodeKind;
import dev.tonyd365.venuesound.audio.RouterState;
import net.minecraft.core.BlockPos;
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

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SPEAKER_BE, pos, state);
    }

    @Override
    public String deviceKey() {
        return getBlockState().getBlock() instanceof PaSpeakerBlock ? "pa_speaker" : "speaker";
    }

    @Override
    public NodeKind kind() {
        return NodeKind.SPEAKER;
    }

    public boolean isOn() {
        return on;
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
    protected RouterState.Node snapshot(ServerLevel level, List<DimPos> outs) {
        double height = getBlockState().getBlock() instanceof PaSpeakerBlock ? 1.0D : 0.5D;
        return new RouterState.Node(dimPos(), kind(), outs, 0, null, 0F, on ? range : 0, height);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Range", range);
        tag.putBoolean("On", on);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        range = tag.contains("Range") ? tag.getInt("Range") : 64;
        on = !tag.contains("On") || tag.getBoolean("On");
    }
}
