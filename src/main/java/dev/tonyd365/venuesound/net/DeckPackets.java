package dev.tonyd365.venuesound.net;

import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.audio.MediaPlayback;
import dev.tonyd365.venuesound.block.MediaDeckBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** Opens the media deck screen and applies its buttons on the server. */
public final class DeckPackets {

    public static final ResourceLocation OPEN = VenueSound.id("open_deck");
    public static final ResourceLocation ACTION = VenueSound.id("deck_action");

    public static final int SELECT = 0;
    public static final int PLAY = 1;
    public static final int STOP = 2;
    public static final int LOOP = 3;

    private static final double REACH = 8.0D;

    private DeckPackets() {
    }

    public static void openScreen(ServerPlayer player, MediaDeckBlockEntity deck) {
        List<String> tracks = MediaPlayback.listTracks();
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(deck.getBlockPos());
        buf.writeVarInt(tracks.size());
        for (String t : tracks) {
            buf.writeUtf(t);
        }
        buf.writeUtf(deck.getTrack());
        buf.writeBoolean(deck.isLoop());
        buf.writeBoolean(MediaPlayback.get().isPlaying(deck.dimPos()));
        ServerPlayNetworking.send(player, OPEN, buf);
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(ACTION, (server, player, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int action = buf.readVarInt();
            String value = buf.readUtf(256);
            server.execute(() -> {
                if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > REACH * REACH) {
                    return;
                }
                if (!(player.level().getBlockEntity(pos) instanceof MediaDeckBlockEntity deck)) {
                    return;
                }
                switch (action) {
                    case SELECT -> {
                        if (MediaPlayback.listTracks().contains(value)) {
                            deck.setTrack(value);
                        }
                    }
                    case PLAY -> deck.play();
                    case STOP -> deck.stop();
                    case LOOP -> deck.setLoop("true".equals(value));
                    default -> {
                    }
                }
            });
        });
    }
}
