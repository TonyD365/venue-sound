package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.RouterState;
import dev.tonyd365.venuesound.audio.SoundRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class MixerBlockEntity extends RoutedBlockEntity {

    public static final int INPUTS = 8;
    public static final float MAX_GAIN = 2.0F;

    private final float[] gains = new float[INPUTS];
    private final boolean[] mutes = new boolean[INPUTS];
    private float master = 1.0F;

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MIXER_BE, pos, state);
        for (int i = 0; i < INPUTS; i++) {
            gains[i] = 1.0F;
        }
    }

    public float getGain(int input) {
        return gains[input];
    }

    public boolean isMuted(int input) {
        return mutes[input];
    }

    public float getMaster() {
        return master;
    }

    /** Applies a change from the mixer screen. {@code input} 0-7 is a channel, 8 is the master fader. */
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
    protected void publish(ServerLevel level) {
        SoundRouter.get().putMixer(new RouterState.Mixer(dimPos(), gains.clone(), mutes.clone(), master));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < INPUTS; i++) {
            tag.putFloat("Gain" + i, gains[i]);
            tag.putBoolean("Mute" + i, mutes[i]);
        }
        tag.putFloat("Master", master);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < INPUTS; i++) {
            gains[i] = tag.contains("Gain" + i) ? tag.getFloat("Gain" + i) : 1.0F;
            mutes[i] = tag.getBoolean("Mute" + i);
        }
        master = tag.contains("Master") ? tag.getFloat("Master") : 1.0F;
    }
}
