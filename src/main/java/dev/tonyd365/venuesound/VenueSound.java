package dev.tonyd365.venuesound;

import dev.tonyd365.venuesound.audio.SoundRouter;
import dev.tonyd365.venuesound.audio.MediaPlayback;
import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
import dev.tonyd365.venuesound.net.DeckPackets;
import dev.tonyd365.venuesound.net.MixerPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VenueSound implements ModInitializer {

    public static final String MOD_ID = "venuesound";
    public static final Logger LOGGER = LoggerFactory.getLogger("VenueSound");

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Registration.register();
        MixerPackets.registerServer();
        DeckPackets.registerServer();

        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
            if (blockEntity instanceof AudioNodeBlockEntity routed) {
                routed.syncToRouter();
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((blockEntity, level) -> {
            if (blockEntity instanceof AudioNodeBlockEntity routed) {
                routed.removeFromRouter();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            MediaPlayback.get().stopAll();
            SoundRouter.get().clear();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> MediaPlayback.mediaFolder());

        LOGGER.info("VenueSound loaded");
    }
}
