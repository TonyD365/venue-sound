package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.net.MixerPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;

public class VenueSoundClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(MixerPackets.OPEN, (client, handler, buf, responseSender) -> {
            BlockPos pos = buf.readBlockPos();
            float[] gains = new float[MixerBlockEntity.INPUTS];
            boolean[] mutes = new boolean[MixerBlockEntity.INPUTS];
            for (int i = 0; i < MixerBlockEntity.INPUTS; i++) {
                gains[i] = buf.readFloat();
                mutes[i] = buf.readBoolean();
            }
            float master = buf.readFloat();
            int patchedMask = buf.readVarInt();
            int speakers = buf.readVarInt();
            client.execute(() -> client.setScreen(new MixerScreen(pos, gains, mutes, master, patchedMask, speakers)));
        });
    }
}
