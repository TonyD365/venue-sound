package dev.tonyd365.venuesound.net;

import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.MixerLayout;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Client to server: set one console control to a value, from a remote controller (OSC). The
 * server only accepts knobs, faders and toggle buttons that exist on that console, clamps the
 * value to the control's range, and only from players within {@link #RANGE} blocks.
 */
public final class ParamPackets {

    public static final ResourceLocation SET = VenueSound.id("set_console_param");
    /** A remote works across the room, not across the world. */
    public static final double RANGE = 64.0D;

    private ParamPackets() {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(SET, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            String param = buf.readUtf(64);
            float value = buf.readFloat();
            server.execute(() -> {
                if (!Float.isFinite(value) || player.distanceToSqr(Vec3.atCenterOf(pos)) > RANGE * RANGE
                        || !(player.level().getBlockEntity(pos) instanceof MixerBlockEntity m)) {
                    return;
                }
                MixerLayout.Control c = m.layout().byParam(param);
                if (c == null || !m.layout().defaults().containsKey(param)) {
                    return;
                }
                float v = switch (c.kind()) {
                    case FADER -> Math.max(0F, Math.min(2F, value));
                    case KNOB -> (float) Math.max(c.min(), Math.min(c.max(), value));
                    case BUTTON -> value >= 0.5F ? 1F : 0F;
                    default -> m.get(param);
                };
                m.set(param, v);
            });
        });
    }
}
