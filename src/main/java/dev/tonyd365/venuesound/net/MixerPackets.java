package dev.tonyd365.venuesound.net;

import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.audio.SoundRouter;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/** Opens the mixer screen on the client and applies fader moves on the server. */
public final class MixerPackets {

    public static final ResourceLocation OPEN = VenueSound.id("open_mixer");
    public static final ResourceLocation UPDATE = VenueSound.id("update_mixer");

    /** Players must stay this close (in blocks) to a mixer to change it. */
    private static final double REACH = 8.0D;

    private MixerPackets() {
    }

    public static void openScreen(ServerPlayer player, MixerBlockEntity mixer) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(mixer.getBlockPos());
        for (int i = 0; i < MixerBlockEntity.INPUTS; i++) {
            buf.writeFloat(mixer.getGain(i));
            buf.writeBoolean(mixer.isMuted(i));
        }
        buf.writeFloat(mixer.getMaster());
        Set<Integer> patched = SoundRouter.get().patchedChannels(mixer.dimPos());
        int mask = 0;
        for (int channel : patched) {
            mask |= 1 << (channel - 1);
        }
        buf.writeVarInt(mask);
        buf.writeVarInt(SoundRouter.get().countSpeakers(mixer.dimPos()));
        ServerPlayNetworking.send(player, OPEN, buf);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(UPDATE, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int input = buf.readVarInt();
            float gain = buf.readFloat();
            boolean mute = buf.readBoolean();
            server.execute(() -> {
                if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > REACH * REACH) {
                    return;
                }
                if (player.level().getBlockEntity(pos) instanceof MixerBlockEntity mixer) {
                    mixer.apply(input, gain, mute);
                }
            });
        });
    }
}
