package dev.tonyd365.venuesound;

import dev.tonyd365.venuesound.block.AmplifierBlock;
import dev.tonyd365.venuesound.block.AmplifierBlockEntity;
import dev.tonyd365.venuesound.block.MediaDeckBlock;
import dev.tonyd365.venuesound.block.MediaDeckBlockEntity;
import dev.tonyd365.venuesound.block.MixerBlock;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.OutletBlock;
import dev.tonyd365.venuesound.block.PaSpeakerBlock;
import dev.tonyd365.venuesound.block.ReceiverBlock;
import dev.tonyd365.venuesound.block.ReceiverBlockEntity;
import dev.tonyd365.venuesound.block.SimpleNodeBlockEntity;
import dev.tonyd365.venuesound.block.SpeakerBlock;
import dev.tonyd365.venuesound.block.SpeakerBlockEntity;
import dev.tonyd365.venuesound.block.StandMicBlock;
import dev.tonyd365.venuesound.item.AudioCableItem;
import dev.tonyd365.venuesound.item.WirelessMicItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class Registration {

    private Registration() {
    }

    private static BlockBehaviour.Properties deviceProps() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK)
                .strength(1.5F, 6.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    public static final ReceiverBlock RECEIVER = new ReceiverBlock(deviceProps());
    public static final StandMicBlock STAND_MIC = new StandMicBlock(deviceProps());
    public static final MediaDeckBlock MEDIA_DECK = new MediaDeckBlock(deviceProps());
    public static final OutletBlock OUTLET = new OutletBlock(deviceProps().strength(0.8F));
    public static final MixerBlock MIXER = new MixerBlock(deviceProps());
    public static final AmplifierBlock AMPLIFIER = new AmplifierBlock(deviceProps());
    public static final SpeakerBlock SPEAKER = new SpeakerBlock(deviceProps());
    public static final PaSpeakerBlock PA_SPEAKER = new PaSpeakerBlock(deviceProps());

    public static final WirelessMicItem WIRELESS_MIC = new WirelessMicItem(new Item.Properties().stacksTo(1));
    public static final AudioCableItem AUDIO_CABLE = new AudioCableItem(new Item.Properties().stacksTo(1));

    public static BlockEntityType<ReceiverBlockEntity> RECEIVER_BE;
    public static BlockEntityType<SimpleNodeBlockEntity> SIMPLE_NODE_BE;
    public static BlockEntityType<MediaDeckBlockEntity> MEDIA_DECK_BE;
    public static BlockEntityType<MixerBlockEntity> MIXER_BE;
    public static BlockEntityType<AmplifierBlockEntity> AMPLIFIER_BE;
    public static BlockEntityType<SpeakerBlockEntity> SPEAKER_BE;

    public static void register() {
        block("receiver", RECEIVER);
        block("stand_mic", STAND_MIC);
        block("media_deck", MEDIA_DECK);
        block("outlet", OUTLET);
        block("mixer", MIXER);
        block("amplifier", AMPLIFIER);
        block("speaker", SPEAKER);
        block("pa_speaker", PA_SPEAKER);

        Registry.register(BuiltInRegistries.ITEM, VenueSound.id("wireless_mic"), WIRELESS_MIC);
        Registry.register(BuiltInRegistries.ITEM, VenueSound.id("audio_cable"), AUDIO_CABLE);

        RECEIVER_BE = be("receiver", FabricBlockEntityTypeBuilder.create(ReceiverBlockEntity::new, RECEIVER).build());
        SIMPLE_NODE_BE = be("simple_node", FabricBlockEntityTypeBuilder.create(SimpleNodeBlockEntity::new, OUTLET, STAND_MIC).build());
        MEDIA_DECK_BE = be("media_deck", FabricBlockEntityTypeBuilder.create(MediaDeckBlockEntity::new, MEDIA_DECK).build());
        MIXER_BE = be("mixer", FabricBlockEntityTypeBuilder.create(MixerBlockEntity::new, MIXER).build());
        AMPLIFIER_BE = be("amplifier", FabricBlockEntityTypeBuilder.create(AmplifierBlockEntity::new, AMPLIFIER).build());
        SPEAKER_BE = be("speaker", FabricBlockEntityTypeBuilder.create(SpeakerBlockEntity::new, SPEAKER, PA_SPEAKER).build());

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, VenueSound.id("main"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.venuesound.main"))
                        .icon(() -> new ItemStack(MIXER))
                        .displayItems((params, output) -> {
                            output.accept(WIRELESS_MIC);
                            output.accept(RECEIVER);
                            output.accept(STAND_MIC);
                            output.accept(MEDIA_DECK);
                            output.accept(AUDIO_CABLE);
                            output.accept(OUTLET);
                            output.accept(MIXER);
                            output.accept(AMPLIFIER);
                            output.accept(PA_SPEAKER);
                            output.accept(SPEAKER);
                        })
                        .build());
    }

    private static <T extends BlockEntityType<?>> T be(String name, T type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, VenueSound.id(name), type);
    }

    private static void block(String name, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, VenueSound.id(name), block);
        Registry.register(BuiltInRegistries.ITEM, VenueSound.id(name), new BlockItem(block, new Item.Properties()));
    }
}
