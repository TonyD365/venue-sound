package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.net.DeckPackets;
import net.fabricmc.api.ClientModInitializer;
import dev.tonyd365.venuesound.compat.ClientNet;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

public class VenueSoundClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        SelfTest.register();
        //? if <1.21 {
        // photo and film modes (docs images and video) are only on 1.20.1
        PhotoShoot.register();
        FilmMode.register();
        //?}
        OscBridge.register();
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(HeldCableRenderer::render);
        BlockEntityRenderers.register(Registration.RECEIVER_BE, ctx -> new DeviceRenderers.Receiver());
        BlockEntityRenderers.register(Registration.SIMPLE_NODE_BE, ctx -> new CableRenderer<>());
        BlockEntityRenderers.register(Registration.MEDIA_DECK_BE, ctx -> new DeviceRenderers.Deck());
        BlockEntityRenderers.register(Registration.VIDEO_CAPTURE_BE, ctx -> new CableRenderer<>());
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(ZoneOutlineRenderer::render);
        //? if >=1.21 {
        /*net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((g, delta) -> ControlHud.render(g, delta.getGameTimeDeltaPartialTick(false)));
        *///?} else
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(ControlHud::render);
        BlockEntityRenderers.register(Registration.MIXER_BE, ctx -> new MixerRenderer());
        BlockEntityRenderers.register(Registration.AMPLIFIER_BE, ctx -> new DeviceRenderers.Amplifier());
        BlockEntityRenderers.register(Registration.SPEAKER_BE, ctx -> new DeviceRenderers.Speaker());

        ClientNet.receive(dev.tonyd365.venuesound.net.MeterPackets.LEVELS, (client, buf) -> {
            BlockPos console = buf.readBlockPos();
            float[] levels = new float[Math.min(64, buf.readVarInt())];
            for (int i = 0; i < levels.length; i++) {
                levels[i] = buf.readUnsignedByte() / 255F;
            }
            ConsoleMeters.update(console, levels);
        });
        ClientNet.receive(DeckPackets.OPEN, (client, buf) -> {
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
