package dev.tonyd365.venuesound.audio;

import org.jetbrains.annotations.Nullable;

/** Immutable snapshots of device settings, published by block entities and read on the voice thread. */
public final class RouterState {

    private RouterState() {
    }

    /** A receiver listening on {@code channel} (1-8) and feeding the mixer at {@code mixer}. */
    public record Receiver(DimPos pos, int channel, @Nullable DimPos mixer) {
    }

    /** A mixer: per-input gain and mute, plus master gain. Arrays are never mutated after publishing. */
    public record Mixer(DimPos pos, float[] gains, boolean[] mutes, float master) {

        public float effectiveGain(int channel) {
            int i = channel - 1;
            if (i < 0 || i >= gains.length || mutes[i]) {
                return 0F;
            }
            return gains[i] * master;
        }
    }

    /** A PA speaker fed by the mixer at {@code mixer}, audible within {@code range} blocks. */
    public record Speaker(DimPos pos, @Nullable DimPos mixer, int range) {
    }
}
