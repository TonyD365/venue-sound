package dev.tonyd365.venuesound.compat;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** The client side of {@link Net}. */
public final class ClientNet {

    private ClientNet() {
    }

    public interface ClientReceiver {
        /** Called with the buffer of a packet the server sent. */
        void receive(Minecraft client, FriendlyByteBuf buf);
    }

    public static void receive(ResourceLocation id, ClientReceiver receiver) {
        //? if >=1.20.5 {
        /*ClientPlayNetworking.registerGlobalReceiver(Net.type(id), (payload, ctx) -> receiver.receive(ctx.client(), Net.buf(payload)));
        *///?} else
        ClientPlayNetworking.registerGlobalReceiver(id, (client, handler, buf, sender) -> receiver.receive(client, buf));
    }

    public static void send(ResourceLocation id, FriendlyByteBuf buf) {
        //? if >=1.20.5 {
        /*ClientPlayNetworking.send(Net.payload(id, buf));
        *///?} else
        ClientPlayNetworking.send(id, buf);
    }
}
