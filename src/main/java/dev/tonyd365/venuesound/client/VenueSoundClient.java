package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.net.DeckPackets;
import dev.tonyd365.venuesound.net.MixerPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class VenueSoundClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(Registration.RECEIVER_BE, ctx -> new DeviceRenderers.Receiver());
        BlockEntityRenderers.register(Registration.SIMPLE_NODE_BE, ctx -> new CableRenderer<>());
        BlockEntityRenderers.register(Registration.MEDIA_DECK_BE, ctx -> new DeviceRenderers.Deck());
        BlockEntityRenderers.register(Registration.MIXER_BE, ctx -> new MixerRenderer());
        BlockEntityRenderers.register(Registration.AMPLIFIER_BE, ctx -> new DeviceRenderers.Amplifier());
        BlockEntityRenderers.register(Registration.SPEAKER_BE, ctx -> new DeviceRenderers.Speaker());

        ClientPlayNetworking.registerGlobalReceiver(MixerPackets.OPEN, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int n = MixerBlockEntity.INPUTS;
            float[] gains = new float[n];
            boolean[] mutes = new boolean[n];
            for (int i = 0; i < n; i++) {
                gains[i] = buf.readFloat();
                mutes[i] = buf.readBoolean();
            }
            float master = buf.readFloat();
            int[] kinds = new int[n];
            int[] channels = new int[n];
            for (int i = 0; i < n; i++) {
                kinds[i] = buf.readVarInt();
                channels[i] = buf.readVarInt();
            }
            int speakers = buf.readVarInt();
            client.execute(() -> client.setScreen(new MixerScreen(pos, gains, mutes, master, kinds, channels, speakers)));
        });

        ClientPlayNetworking.registerGlobalReceiver(DeckPackets.OPEN, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            int count = buf.readVarInt();
            List<String> tracks = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                tracks.add(buf.readUtf());
            }
            String current = buf.readUtf();
            boolean loop = buf.readBoolean();
            boolean playing = buf.readBoolean();
            client.execute(() -> client.setScreen(new DeckScreen(pos, tracks, current, loop, playing)));
        });
    }
}
