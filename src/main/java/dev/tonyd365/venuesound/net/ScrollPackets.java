package dev.tonyd365.venuesound.net;

import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.block.ScrollControl;
import dev.tonyd365.venuesound.compat.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Client to server: the mouse wheel turned the knob or fader under the crosshair. */
public final class ScrollPackets {

    public static final ResourceLocation SCROLL = VenueSound.id("scroll_control");
    private static final double REACH = 8.0D;

    private ScrollPackets() {
    }

    public static void registerServer() {
        Net.toServer(SCROLL);
        Net.receive(SCROLL, (server, player, buf) -> {
            BlockPos pos = buf.readBlockPos();
            Direction face = buf.readEnum(Direction.class);
            Vec3 hit = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
            int dir = buf.readByte() > 0 ? 1 : -1;
            server.execute(() -> {
                if (player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH * REACH || hit.distanceToSqr(Vec3.atCenterOf(pos)) > 4) {
                    return;
                }
                ScrollControl.Target t = ScrollControl.find(player.level(), pos, face, hit);
                if (t != null) {
                    ScrollControl.apply(player.level(), t, dir);
                }
            });
        });
    }
}
