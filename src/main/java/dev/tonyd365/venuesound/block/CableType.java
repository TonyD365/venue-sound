package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.audio.NodeKind;
import org.jetbrains.annotations.Nullable;

/**
 * The kinds of cable, each with the plugs real cables have. A cable plugs into a socket only if
 * the socket takes its plug (see {@link PortLayout.Connector}): XLR into XLR and combo sockets,
 * TRS into TRS and combo sockets, Speakon only into amplifier outputs and passive speakers, the
 * multicore snake from a stage box into console inputs. Each type also has its own reach and
 * colour.
 */
public enum CableType {
    /** 6.35 mm TRS line / instrument cable. */
    AUDIO("audio_cable", 64, 0x16161A, 0.035F, PortLayout.Connector.TRS),
    XLR("xlr_cable", 96, 0x2B3A55, 0.035F, PortLayout.Connector.XLR),
    SPEAKER("speaker_cable", 96, 0xD9822B, 0.045F, PortLayout.Connector.SPEAKON),
    SNAKE("snake_cable", 128, 0x23262B, 0.07F, PortLayout.Connector.MULTIPIN),
    /** A long XLR cable on a drum. */
    REEL("cable_reel", 128, 0xE0B526, 0.035F, PortLayout.Connector.XLR);

    /** Item id without the namespace. */
    public final String id;
    /** Longest single run (socket or clip to the next clip or socket), in blocks. */
    public final int maxLength;
    /** Colour in the world, 0xRRGGBB. */
    public final int rgb;
    /** Half the drawn thickness, in blocks. */
    public final float halfWidth;
    /** The plug on both ends. */
    public final PortLayout.Connector plug;

    CableType(String id, int maxLength, int rgb, float halfWidth, PortLayout.Connector plug) {
        this.id = id;
        this.maxLength = maxLength;
        this.rgb = rgb;
        this.halfWidth = halfWidth;
        this.plug = plug;
    }

    public static CableType byId(String id) {
        for (CableType t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return AUDIO;
    }

    /** Whether this cable's plug goes into a socket of the given connector. */
    public boolean fits(PortLayout.Connector socket) {
        return socket.takes(plug);
    }

    /** The multicore snake fans out into a console (or a wall outlet), nothing else: a message key, or null. */
    @Nullable
    public String refuseTarget(NodeKind toKind) {
        return this == SNAKE && toKind != NodeKind.MIXER && toKind != NodeKind.OUTLET ? "message.venuesound.cable_snake_only" : null;
    }
}
