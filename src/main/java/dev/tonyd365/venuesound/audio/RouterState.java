package dev.tonyd365.venuesound.audio;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Immutable snapshots of device settings, published by block entities and read on audio threads. */
public final class RouterState {

    private RouterState() {
    }

    /**
     * One device. Only the fields relevant to {@link #kind()} are meaningful.
     *
     * @param outputs   devices this one is cabled to
     * @param channel   receiver channel (1-8)
     * @param mixer     mixer settings, for MIXER
     * @param ampLevel  amplifier gain, for AMPLIFIER (0 when switched off)
     * @param range     speaker coverage in blocks, for SPEAKER
     * @param heightOff speaker sound height above the block position, for SPEAKER
     */
    public record Node(DimPos pos, NodeKind kind, List<DimPos> outputs, int channel,
                       @Nullable Mixer mixer, float ampLevel, int range, double heightOff) {
    }

    /** Mixer settings. {@code inputs[i]} is the device cabled into input i, or null. Arrays are never mutated. */
    public record Mixer(float[] gains, boolean[] mutes, float master, DimPos[] inputs) {

        public float gainFor(DimPos upstream) {
            for (int i = 0; i < inputs.length; i++) {
                if (upstream.equals(inputs[i])) {
                    return mutes[i] ? 0F : gains[i] * master;
                }
            }
            return 0F;
        }
    }
}
