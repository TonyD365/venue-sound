package dev.tonyd365.venuesound;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import dev.tonyd365.venuesound.audio.SoundRouter;
import org.jetbrains.annotations.Nullable;

/**
 * Simple Voice Chat plugin entrypoint ("voicechat" in fabric.mod.json).
 */
public class VenueSoundVoicechatPlugin implements VoicechatPlugin {

    public static final String CATEGORY = "venuesound";

    @Nullable
    private static volatile VoicechatServerApi serverApi;

    @Nullable
    public static VoicechatServerApi serverApi() {
        return serverApi;
    }

    @Override
    public String getPluginId() {
        return VenueSound.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> {
            SoundRouter.get().clear();
            serverApi = null;
        });
        registration.registerEvent(MicrophonePacketEvent.class, event -> SoundRouter.get().onMicrophonePacket(event));
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        VoicechatServerApi api = event.getVoicechat();
        serverApi = api;
        VolumeCategory category = api.volumeCategoryBuilder()
                .setId(CATEGORY)
                .setName("VenueSound PA")
                .setDescription("Volume of VenueSound PA speakers")
                .build();
        api.registerVolumeCategory(category);
    }
}
