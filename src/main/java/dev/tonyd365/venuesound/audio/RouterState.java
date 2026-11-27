package dev.tonyd365.venuesound.audio;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/** Immutable snapshots of device settings, published by block entities and read on audio threads. */
public final class RouterState {

    private RouterState() {
    }

    /** A cable leaving a device: into {@code target}, from our socket {@code fromPort} into its socket {@code toPort}. */
    public record Out(DimPos target, int fromPort, int toPort) {
    }

    /**
     * One device. Only the fields relevant to {@link #kind()} are meaningful.
     *
     * @param channel   receiver channel (1-8)
     * @param mixer     console settings, for MIXER
     * @param ampLevel  amplifier gain, for AMPLIFIER (0 when switched off)
     * @param range     speaker coverage in blocks, for SPEAKER (0 when switched off)
     * @param heightOff speaker sound height above the block position, for SPEAKER
     */
    public record Node(DimPos pos, NodeKind kind, List<Out> outputs, int channel,
                       @Nullable Mixer mixer, float ampLevel, int range, double heightOff) {
    }

    /** Console settings (see MixerLayout for the keys) and what is plugged into each input. */
    public record Mixer(Map<String, Float> params, DimPos[] inputs) {

        public float get(String key) {
            Float v = params.get(key);
            return v == null ? 0F : v;
        }

        /** The input slot {@code upstream} is plugged into, or -1. */
        public int slotOf(DimPos upstream) {
            for (int i = 0; i < inputs.length; i++) {
                if (upstream.equals(inputs[i])) {
                    return i;
                }
            }
            return -1;
        }
    }
}
