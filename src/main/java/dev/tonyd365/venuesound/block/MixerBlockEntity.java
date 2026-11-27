package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.DimPos;
import dev.tonyd365.venuesound.audio.NodeKind;
import dev.tonyd365.venuesound.audio.RouterState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Mixing console: 8 inputs, each with a fader and mute, plus a master fader. */
public class MixerBlockEntity extends AudioNodeBlockEntity {

    public static final int INPUTS = 8;
    public static final float MAX_GAIN = 2.0F;

    private final float[] gains = new float[INPUTS];
    private final boolean[] mutes = new boolean[INPUTS];
    private final float[] trims = new float[INPUTS];
    private final BlockPos[] inputs = new BlockPos[INPUTS];
    private float master = 1.0F;

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MIXER_BE, pos, state);
        for (int i = 0; i < INPUTS; i++) {
            gains[i] = 1.0F;
            trims[i] = 1.0F;
        }
    }

    @Override
    public String deviceKey() {
        return "mixer";
    }

    @Override
    public NodeKind kind() {
        return NodeKind.MIXER;
    }

    public float getGain(int input) {
        return gains[input];
    }

    public boolean isMuted(int input) {
        return mutes[input];
    }

    /** Input trim (the knob above each mute button), 0-2. */
    public float getTrim(int input) {
        return trims[input];
    }

    /** Turns a trim knob one 10 % step up or down. */
    public void stepTrim(int input, int direction) {
        trims[input] = Math.max(0F, Math.min(MAX_GAIN, Math.round((trims[input] + direction * 0.1F) * 10F) / 10F));
        changed();
    }

    public float getMaster() {
        return master;
    }

    @Nullable
    public BlockPos getInput(int input) {
        return inputs[input];
    }

    /** Plugs {@code upstream} into input {@code slot} (0-7). False if another device is already plugged in there. */
    public boolean assignInput(BlockPos upstream, int slot) {
        if (slot < 0 || slot >= INPUTS || (inputs[slot] != null && !inputs[slot].equals(upstream))) {
            return false;
        }
        releaseInputQuietly(upstream);
        inputs[slot] = upstream.immutable();
        changed();
        return true;
    }

    private void releaseInputQuietly(BlockPos upstream) {
        for (int i = 0; i < INPUTS; i++) {
            if (upstream.equals(inputs[i])) {
                inputs[i] = null;
            }
        }
    }

    /** Plugs {@code upstream} into the first free input. Returns the input number (1-8) or -1 if full. */
    public int assignInput(BlockPos upstream) {
        for (int i = 0; i < INPUTS; i++) {
            if (upstream.equals(inputs[i])) {
                return i + 1;
            }
        }
        for (int i = 0; i < INPUTS; i++) {
            if (inputs[i] == null) {
                inputs[i] = upstream.immutable();
                changed();
                return i + 1;
            }
        }
        return -1;
    }

    public void releaseInput(BlockPos upstream) {
        for (int i = 0; i < INPUTS; i++) {
            if (upstream.equals(inputs[i])) {
                inputs[i] = null;
            }
        }
        changed();
    }

    public void releaseInputSlot(int slot) {
        if (slot >= 0 && slot < INPUTS && inputs[slot] != null) {
            inputs[slot] = null;
            changed();
        }
    }

    public void clearInputs() {
        for (int i = 0; i < INPUTS; i++) {
            inputs[i] = null;
        }
        changed();
    }

    /** Applies a change from the console screen. {@code input} 0-7 is a channel, 8 is the master fader. */
    public void apply(int input, float gain, boolean mute) {
        gain = Math.max(0F, Math.min(MAX_GAIN, gain));
        if (input == INPUTS) {
            master = gain;
        } else if (input >= 0 && input < INPUTS) {
            gains[input] = gain;
            mutes[input] = mute;
        } else {
            return;
        }
        changed();
    }

    @Override
    protected RouterState.Node snapshot(ServerLevel level, List<DimPos> outs) {
        DimPos[] in = new DimPos[INPUTS];
        for (int i = 0; i < INPUTS; i++) {
            in[i] = inputs[i] == null ? null : new DimPos(level.dimension(), inputs[i]);
        }
        RouterState.Mixer mixer = new RouterState.Mixer(gains.clone(), trims.clone(), mutes.clone(), master, in);
        return new RouterState.Node(dimPos(), kind(), outs, 0, mixer, 0F, 0, 0.5D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < INPUTS; i++) {
            tag.putFloat("Gain" + i, gains[i]);
            tag.putBoolean("Mute" + i, mutes[i]);
            tag.putFloat("Trim" + i, trims[i]);
            if (inputs[i] != null) {
                tag.putLong("Input" + i, inputs[i].asLong());
            }
        }
        tag.putFloat("Master", master);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < INPUTS; i++) {
            gains[i] = tag.contains("Gain" + i) ? tag.getFloat("Gain" + i) : 1.0F;
            mutes[i] = tag.getBoolean("Mute" + i);
            trims[i] = tag.contains("Trim" + i) ? tag.getFloat("Trim" + i) : 1.0F;
            inputs[i] = tag.contains("Input" + i) ? BlockPos.of(tag.getLong("Input" + i)) : null;
        }
        master = tag.contains("Master") ? tag.getFloat("Master") : 1.0F;
    }
}
