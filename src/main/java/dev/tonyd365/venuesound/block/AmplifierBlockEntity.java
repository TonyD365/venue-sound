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

/** Power amplifier between the mixer and the speakers: power switch and output level. */
public class AmplifierBlockEntity extends AudioNodeBlockEntity {

    /** Output level steps, cycled by right-clicking. */
    public static final int[] LEVELS = PanelLayout.Amp.LEVELS;

    private boolean on = true;
    private int levelPercent = 100;

    public AmplifierBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.AMPLIFIER_BE, pos, state);
    }

    @Override
    public String deviceKey() {
        return getBlockState().getBlock() instanceof ProcessorBlock p ? p.key : "amplifier";
    }

    @Override
    public NodeKind kind() {
        return getBlockState().getBlock() instanceof ProcessorBlock ? NodeKind.PROCESSOR : NodeKind.AMPLIFIER;
    }

    public boolean isOn() {
        return on;
    }

    public int getLevelPercent() {
        return levelPercent;
    }

    public void togglePower() {
        on = !on;
        changed();
    }

    /** Turns the level knob one step down or up (it stops at the ends, like a real knob). */
    public void cycleLevel(boolean down) {
        int index = 3;
        for (int i = 0; i < LEVELS.length; i++) {
            if (LEVELS[i] == levelPercent) {
                index = i;
            }
        }
        int next = Math.max(0, Math.min(LEVELS.length - 1, index + (down ? -1 : 1)));
        if (next != index) {
            levelPercent = LEVELS[next];
            changed();
        }
    }

    @Override
    protected RouterState.Node snapshot(ServerLevel level, List<RouterState.Out> outs) {
        return new RouterState.Node(dimPos(), kind(), outs, 0, null, on ? levelPercent / 100F : 0F, 0, 0.5D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("On", on);
        tag.putInt("Level", levelPercent);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        on = !tag.contains("On") || tag.getBoolean("On");
        levelPercent = tag.contains("Level") ? tag.getInt("Level") : 100;
    }
}
