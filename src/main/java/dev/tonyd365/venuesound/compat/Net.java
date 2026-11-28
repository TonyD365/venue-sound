package dev.tonyd365.venuesound.compat;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * VenueSound's packets on every Minecraft version. Each packet is an id and a FriendlyByteBuf
 * written and read by the packet classes in {@code net/}: sent as is on 1.20.1, carried in a
 * custom payload ({@link Raw}) since 1.20.5.
 */
public final class Net {

    private Net() {
    }

    public interface ServerReceiver {
        /** Called with the buffer of a packet a player sent. */
        void receive(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf);
    }

    //? if >=1.20.5 {
    /*/^* A packet's bytes, as a custom payload of its own type. ^/
    public record Raw(net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Raw> type, byte[] data) implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {
    }

    private static final java.util.Map<ResourceLocation, net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Raw>> TYPES =
            new java.util.concurrent.ConcurrentHashMap<>();

    public static net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Raw> type(ResourceLocation id) {
        return TYPES.computeIfAbsent(id, net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type::new);
    }

    private static net.minecraft.network.codec.StreamCodec<FriendlyByteBuf, Raw> codec(ResourceLocation id) {
        var type = type(id);
        return net.minecraft.network.codec.StreamCodec.of((buf, raw) -> buf.writeBytes(raw.data()), buf -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            return new Raw(type, data);
        });
    }

    public static Raw payload(ResourceLocation id, FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return new Raw(type(id), data);
    }

    public static FriendlyByteBuf buf(Raw raw) {
        return new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(raw.data()));
    }
    *///?}

    /** Registers a client-to-server packet (on both sides, before any receiver). */
    public static void toServer(ResourceLocation id) {
        //? if >=1.20.5
        //net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playC2S().register(type(id), codec(id));
    }

    /** Registers a server-to-client packet (on both sides, before any receiver). */
    public static void toClient(ResourceLocation id) {
        //? if >=1.20.5
        //net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(type(id), codec(id));
    }

    public static void receive(ResourceLocation id, ServerReceiver receiver) {
        //? if >=1.20.5 {
        /*ServerPlayNetworking.registerGlobalReceiver(type(id), (payload, ctx) -> receiver.receive(ctx.server(), ctx.player(), buf(payload)));
        *///?} else
        ServerPlayNetworking.registerGlobalReceiver(id, (server, player, handler, buf, sender) -> receiver.receive(server, player, buf));
    }

    public static void send(ServerPlayer player, ResourceLocation id, FriendlyByteBuf buf) {
        //? if >=1.20.5 {
        /*ServerPlayNetworking.send(player, payload(id, buf));
        *///?} else
        ServerPlayNetworking.send(player, id, buf);
    }
}
