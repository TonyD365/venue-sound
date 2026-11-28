package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.audio.NodeKind;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of cable. The plain audio cable connects anything; the others each do one job, like
 * their real counterparts, and reach further and look different in the world.
 *
 * <ul>
 *   <li>XLR mic cable: mic- and line-level signal (mics, receivers, players, processors, consoles,
 *       powered speakers); never the amplifier's speaker-level outputs.</li>
 *   <li>Speaker cable: amplifier OUT to a speaker's INPUT (through wall outlets if needed).</li>
 *   <li>Multicore snake: a stage box's SNAKE socket to the console, carrying all its channels.</li>
 *   <li>Cable reel: a long general-purpose cable for runs across the venue.</li>
 * </ul>
 */
public enum CableType {
    AUDIO("audio_cable", 64, 0x16161A, 0.035F),
    XLR("xlr_cable", 96, 0x2B3A55, 0.035F),
    SPEAKER("speaker_cable", 96, 0xD9822B, 0.045F),
    SNAKE("snake_cable", 128, 0x23262B, 0.07F),
    REEL("cable_reel", 128, 0xE0B526, 0.035F);

    /** Item id without the namespace. */
    public final String id;
    /** Longest single run (socket or clip to the next clip or socket), in blocks. */
    public final int maxLength;
    /** Colour in the world, 0xRRGGBB. */
    public final int rgb;
    /** Half the drawn thickness, in blocks. */
    public final float halfWidth;

    CableType(String id, int maxLength, int rgb, float halfWidth) {
        this.id = id;
        this.maxLength = maxLength;
        this.rgb = rgb;
        this.halfWidth = halfWidth;
    }

    public static CableType byId(String id) {
        for (CableType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return AUDIO;
    }

    private static boolean isSnakeOut(String deviceKey, String portLabel) {
        return deviceKey.startsWith("stage_box") && "SNAKE".equals(portLabel);
    }

    /**
     * Why this cable can't start at the given OUT socket, as a message key, or null if it can.
     */
    @Nullable
    public String refuseStart(String fromKey, NodeKind fromKind, String fromLabel) {
        return switch (this) {
            case XLR -> fromKind == NodeKind.AMPLIFIER ? "message.venuesound.cable_xlr_amp" : null;
            case SPEAKER -> fromKind == NodeKind.AMPLIFIER || (fromKind == NodeKind.OUTLET && !fromKey.startsWith("stage_box"))
                    ? null : "message.venuesound.cable_speaker_only";
            case SNAKE -> isSnakeOut(fromKey, fromLabel) ? null : "message.venuesound.cable_snake_only";
            default -> null;
        };
    }

    /**
     * Why this cable can't join the two sockets, as a message key, or null if it can.
     */
    @Nullable
    public String refuse(String fromKey, NodeKind fromKind, String fromLabel, String toKey, NodeKind toKind) {
        String start = refuseStart(fromKey, fromKind, fromLabel);
        if (start != null) {
            return start;
        }
        return switch (this) {
            case SPEAKER -> toKind == NodeKind.SPEAKER || toKind == NodeKind.OUTLET ? null : "message.venuesound.cable_speaker_only";
            case SNAKE -> toKind == NodeKind.MIXER || toKind == NodeKind.OUTLET ? null : "message.venuesound.cable_snake_only";
            default -> null;
        };
    }
}
