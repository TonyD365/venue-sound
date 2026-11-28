package dev.tonyd365.venuesound.net;

import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.audio.DimPos;
import dev.tonyd365.venuesound.block.MixerLayout;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import dev.tonyd365.venuesound.compat.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live levels for the consoles' meters and PEAK LEDs. The voice and media threads report every
 * frame a console mixes; every 2 ticks the loudest level since the last send goes to the players
 * near that console.
 */
public final class MeterPackets {

    public static final ResourceLocation LEVELS = VenueSound.id("console_levels");
    /** Players this close to a console get its levels. */
    private static final double RANGE = 48.0D;

    /** Per console: [0] MAIN L, [1] MAIN R, [2 + strip] each strip's input. */
    private static final Map<DimPos, float[]> PEAKS = new ConcurrentHashMap<>();

    private MeterPackets() {
    }

    /** Called from the audio threads for every frame a console mixes. */
    public static void report(DimPos console, int strip, float input, float mainL, float mainR) {
        float[] p = PEAKS.computeIfAbsent(console, k -> new float[2 + MixerLayout.MAX_STRIPS]);
        synchronized (p) {
            p[0] = Math.max(p[0], mainL);
            p[1] = Math.max(p[1], mainR);
            if (strip >= 0 && strip < MixerLayout.MAX_STRIPS) {
                p[2 + strip] = Math.max(p[2 + strip], input);
            }
        }
    }

    public static void registerServer() {
        Net.toClient(LEVELS);
        ServerTickEvents.END_SERVER_TICK.register(MeterPackets::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 2 != 0 || PEAKS.isEmpty()) {
            return;
        }
        for (Map.Entry<DimPos, float[]> e : PEAKS.entrySet()) {
            float[] p = e.getValue();
            float[] snapshot;
            synchronized (p) {
                snapshot = p.clone();
                java.util.Arrays.fill(p, 0F);
            }
            boolean silent = true;
            for (float f : snapshot) {
                if (f > 0.0005F) {
                    silent = false;
                    break;
                }
            }
            if (silent) {
                PEAKS.remove(e.getKey(), p);
                continue;
            }
            ServerLevel level = server.getLevel(e.getKey().dimension());
            if (level == null) {
                continue;
            }
            BlockPos pos = e.getKey().pos();
            for (ServerPlayer player : level.players()) {
                if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > RANGE * RANGE) {
                    continue;
                }
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeBlockPos(pos);
                buf.writeVarInt(snapshot.length);
                for (float f : snapshot) {
                    // 0-255 is plenty for an LED meter
                    buf.writeByte((int) Math.min(255, Math.round(Math.min(1F, f) * 255F)));
                }
                Net.send(player, LEVELS, buf);
            }
        }
    }
}
