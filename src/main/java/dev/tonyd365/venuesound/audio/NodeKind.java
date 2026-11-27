package dev.tonyd365.venuesound.audio;

/** Every kind of device in the signal chain, and which kinds a cable may run between. */
public enum NodeKind {
    RECEIVER,
    STAND_MIC,
    MEDIA_DECK,
    OUTLET,
    MIXER,
    AMPLIFIER,
    SPEAKER;

    public boolean isSource() {
        return this == RECEIVER || this == STAND_MIC || this == MEDIA_DECK;
    }

    /** Most cables a device of this kind can send out. */
    public int maxOutputs() {
        return switch (this) {
            case SPEAKER -> 0;
            case RECEIVER, STAND_MIC, MEDIA_DECK -> 1;
            case MIXER -> 4;
            case OUTLET, AMPLIFIER -> 8;
        };
    }

    /** Whether a cable from this kind may plug into {@code target}. */
    public boolean canFeed(NodeKind target) {
        return switch (this) {
            case RECEIVER, STAND_MIC, MEDIA_DECK -> target == OUTLET || target == MIXER;
            case OUTLET -> target == OUTLET || target == MIXER || target == AMPLIFIER || target == SPEAKER;
            case MIXER -> target == OUTLET || target == AMPLIFIER || target == SPEAKER;
            case AMPLIFIER -> target == OUTLET || target == SPEAKER;
            case SPEAKER -> false;
        };
    }
}
