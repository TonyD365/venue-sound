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
    public static final dev.tonyd365.venuesound.block.MixerBlock24 MIXER24 = new dev.tonyd365.venuesound.block.MixerBlock24(deviceProps());
    public static final AmplifierBlock AMPLIFIER = new AmplifierBlock(deviceProps());
    public static final SpeakerBlock SPEAKER = new SpeakerBlock(deviceProps());
    public static final PaSpeakerBlock PA_SPEAKER = new PaSpeakerBlock(deviceProps());
    public static final dev.tonyd365.venuesound.block.ConcertSpeakerBlock CONCERT_SPEAKER = new dev.tonyd365.venuesound.block.ConcertSpeakerBlock(deviceProps());
    public static final dev.tonyd365.venuesound.block.ProcessorBlock GRAPHIC_EQ = new dev.tonyd365.venuesound.block.ProcessorBlock("graphic_eq", deviceProps());
    public static final dev.tonyd365.venuesound.block.ProcessorBlock COMPRESSOR = new dev.tonyd365.venuesound.block.ProcessorBlock("compressor", deviceProps());
    public static final dev.tonyd365.venuesound.block.ProcessorBlock DI_BOX = new dev.tonyd365.venuesound.block.ProcessorBlock("di_box", deviceProps());
    public static final dev.tonyd365.venuesound.block.ProSpeakerBlock FLOOR_WEDGE = new dev.tonyd365.venuesound.block.ProSpeakerBlock("floor_wedge", 0.4D,
            Block.box(0, 0, 0, 16, 9, 16), deviceProps());
    public static final dev.tonyd365.venuesound.block.ProSpeakerBlock SUBWOOFER = new dev.tonyd365.venuesound.block.ProSpeakerBlock("subwoofer", 0.5D,
            Block.box(0, 0, 0, 16, 16, 16), deviceProps());
    public static final StandMicBlock GOOSENECK_MIC = new StandMicBlock("gooseneck_mic", 2, deviceProps());
    public static final StandMicBlock CHOIR_MIC = new StandMicBlock("choir_mic", 8, deviceProps().noCollission());
    public static final dev.tonyd365.venuesound.block.ProcessorBlock FX_PROCESSOR = new dev.tonyd365.venuesound.block.ProcessorBlock("fx_processor", deviceProps());
    public static final dev.tonyd365.venuesound.block.ProcessorBlock CROSSOVER = new dev.tonyd365.venuesound.block.ProcessorBlock("crossover", deviceProps());
    public static final dev.tonyd365.venuesound.block.ProSpeakerBlock FLOWN_ARRAY = new dev.tonyd365.venuesound.block.ProSpeakerBlock("flown_array", 0.5D,
            Block.box(0, 0, 0, 16, 16, 16), deviceProps());
    public static final dev.tonyd365.venuesound.block.ProSpeakerBlock IEM_TRANSMITTER = new dev.tonyd365.venuesound.block.ProSpeakerBlock("iem_transmitter",
            dev.tonyd365.venuesound.audio.SpeakerOutput.IEM - 1, Block.box(1, 0, 3, 15, 5, 13), deviceProps());
    public static final dev.tonyd365.venuesound.block.StageBoxBlock STAGE_BOX = new dev.tonyd365.venuesound.block.StageBoxBlock(deviceProps());
    public static final dev.tonyd365.venuesound.block.StageBoxBlock STAGE_BOX_16 = new dev.tonyd365.venuesound.block.StageBoxBlock(16, deviceProps());
    public static final dev.tonyd365.venuesound.block.SequencerBlock POWER_SEQUENCER = new dev.tonyd365.venuesound.block.SequencerBlock(deviceProps());
    public static final dev.tonyd365.venuesound.block.RackBlock RACK_CABINET = new dev.tonyd365.venuesound.block.RackBlock(deviceProps());
    public static final dev.tonyd365.venuesound.block.RackBlock TRUSS_POINT = new dev.tonyd365.venuesound.block.RackBlock(deviceProps());
    public static final dev.tonyd365.venuesound.block.CableClipBlock CABLE_CLIP = new dev.tonyd365.venuesound.block.CableClipBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.3F).sound(SoundType.METAL).noOcclusion().noCollission());

    public static final WirelessMicItem WIRELESS_MIC = new WirelessMicItem(new Item.Properties().stacksTo(1));
    public static final dev.tonyd365.venuesound.item.BodyPackMicItem HEADSET_MIC = new dev.tonyd365.venuesound.item.BodyPackMicItem(net.minecraft.world.entity.EquipmentSlot.HEAD, new Item.Properties().stacksTo(1));
    public static final dev.tonyd365.venuesound.item.BodyPackMicItem LAVALIER_MIC = new dev.tonyd365.venuesound.item.BodyPackMicItem(net.minecraft.world.entity.EquipmentSlot.CHEST, new Item.Properties().stacksTo(1));
    public static final dev.tonyd365.venuesound.item.ZoneSelectorItem ZONE_SELECTOR = new dev.tonyd365.venuesound.item.ZoneSelectorItem(new Item.Properties().stacksTo(1));
    public static final dev.tonyd365.venuesound.block.VideoCaptureBlock VIDEO_CAPTURE = new dev.tonyd365.venuesound.block.VideoCaptureBlock(deviceProps());
    public static BlockEntityType<dev.tonyd365.venuesound.block.VideoCaptureBlockEntity> VIDEO_CAPTURE_BE;
    public static final Item IEM_EARPIECE = new dev.tonyd365.venuesound.item.EarpieceItem(new Item.Properties().stacksTo(1));
    public static final Item INTERCOM_BELTPACK = new dev.tonyd365.venuesound.item.IntercomBeltpackItem(new Item.Properties().stacksTo(1));
    public static final dev.tonyd365.venuesound.block.ProSpeakerBlock INTERCOM_STATION = new dev.tonyd365.venuesound.block.ProSpeakerBlock("intercom_station",
            dev.tonyd365.venuesound.audio.SpeakerOutput.INTERCOM - 1, Block.box(0, 0, 0, 16, 8, 16), deviceProps());
    public static final AudioCableItem XLR_CABLE = new AudioCableItem(new Item.Properties().stacksTo(1));
    public static final AudioCableItem SPEAKER_CABLE = new AudioCableItem(new Item.Properties().stacksTo(1));
    public static final AudioCableItem SNAKE_CABLE = new AudioCableItem(new Item.Properties().stacksTo(1));
    public static final AudioCableItem CABLE_REEL = new AudioCableItem(new Item.Properties().stacksTo(1));
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
        block("mixer24", MIXER24);
        block("amplifier", AMPLIFIER);
        block("speaker", SPEAKER);
        block("pa_speaker", PA_SPEAKER);
        block("concert_speaker", CONCERT_SPEAKER);
        block("cable_clip", CABLE_CLIP);
        block("graphic_eq", GRAPHIC_EQ);
        block("gooseneck_mic", GOOSENECK_MIC);
        block("choir_mic", CHOIR_MIC);
        block("fx_processor", FX_PROCESSOR);
        block("crossover", CROSSOVER);
        block("flown_array", FLOWN_ARRAY);
        block("iem_transmitter", IEM_TRANSMITTER);
        block("intercom_station", INTERCOM_STATION);
        Registry.register(BuiltInRegistries.ITEM, VenueSound.id("intercom_beltpack"), INTERCOM_BELTPACK);
        block("stage_box", STAGE_BOX);
        block("stage_box_16", STAGE_BOX_16);
        block("video_capture", VIDEO_CAPTURE);
        Registry.register(BuiltInRegistries.ITEM, VenueSound.id("zone_selector"), ZONE_SELECTOR);
        VIDEO_CAPTURE_BE = be("video_capture", FabricBlockEntityTypeBuilder.create(dev.tonyd365.venuesound.block.VideoCaptureBlockEntity::new, VIDEO_CAPTURE).build());
        block("power_sequencer", POWER_SEQUENCER);
        block("rack_cabinet", RACK_CABINET);
        block("truss_point", TRUSS_POINT);
        for (var e : java.util.Map.of("headset_mic", HEADSET_MIC, "lavalier_mic", LAVALIER_MIC, "iem_earpiece", IEM_EARPIECE,
                "xlr_cable", XLR_CABLE, "speaker_cable", SPEAKER_CABLE, "snake_cable", SNAKE_CABLE, "cable_reel", CABLE_REEL).entrySet()) {
            Registry.register(BuiltInRegistries.ITEM, VenueSound.id(e.getKey()), e.getValue());
        }
        block("compressor", COMPRESSOR);
        block("di_box", DI_BOX);
        block("floor_wedge", FLOOR_WEDGE);
        block("subwoofer", SUBWOOFER);

        Registry.register(BuiltInRegistries.ITEM, VenueSound.id("wireless_mic"), WIRELESS_MIC);
        Registry.register(BuiltInRegistries.ITEM, VenueSound.id("audio_cable"), AUDIO_CABLE);

        RECEIVER_BE = be("receiver", FabricBlockEntityTypeBuilder.create(ReceiverBlockEntity::new, RECEIVER).build());
        SIMPLE_NODE_BE = be("simple_node", FabricBlockEntityTypeBuilder.create(SimpleNodeBlockEntity::new, OUTLET, STAND_MIC, GOOSENECK_MIC, CHOIR_MIC, STAGE_BOX, STAGE_BOX_16).build());
        MEDIA_DECK_BE = be("media_deck", FabricBlockEntityTypeBuilder.create(MediaDeckBlockEntity::new, MEDIA_DECK).build());
        MIXER_BE = be("mixer", FabricBlockEntityTypeBuilder.create(MixerBlockEntity::new, MIXER, MIXER24).build());
        AMPLIFIER_BE = be("amplifier", FabricBlockEntityTypeBuilder.create(AmplifierBlockEntity::new, AMPLIFIER, GRAPHIC_EQ, COMPRESSOR, DI_BOX, FX_PROCESSOR, CROSSOVER).build());
        SPEAKER_BE = be("speaker", FabricBlockEntityTypeBuilder.create(SpeakerBlockEntity::new, SPEAKER, PA_SPEAKER, CONCERT_SPEAKER, FLOOR_WEDGE, SUBWOOFER, FLOWN_ARRAY, IEM_TRANSMITTER, INTERCOM_STATION).build());

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, VenueSound.id("main"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.venuesound.main"))
                        .icon(() -> new ItemStack(MIXER))
                        .displayItems((params, output) -> {
                            output.accept(WIRELESS_MIC);
                            output.accept(RECEIVER);
                            output.accept(STAND_MIC);
                            output.accept(MEDIA_DECK);
                            output.accept(HEADSET_MIC);
                            output.accept(LAVALIER_MIC);
                            output.accept(GOOSENECK_MIC);
                            output.accept(CHOIR_MIC);
                            output.accept(AUDIO_CABLE);
                            output.accept(XLR_CABLE);
                            output.accept(SPEAKER_CABLE);
                            output.accept(SNAKE_CABLE);
                            output.accept(CABLE_REEL);
                            output.accept(STAGE_BOX);
                            output.accept(STAGE_BOX_16);
                            output.accept(VIDEO_CAPTURE);
                            output.accept(ZONE_SELECTOR);
                            output.accept(FX_PROCESSOR);
                            output.accept(CROSSOVER);
                            output.accept(POWER_SEQUENCER);
                            output.accept(RACK_CABINET);
                            output.accept(IEM_TRANSMITTER);
                            output.accept(IEM_EARPIECE);
                            output.accept(INTERCOM_STATION);
                            output.accept(INTERCOM_BELTPACK);
                            output.accept(FLOWN_ARRAY);
                            output.accept(TRUSS_POINT);
                            output.accept(CABLE_CLIP);
                            output.accept(OUTLET);
                            output.accept(MIXER);
                            output.accept(MIXER24);
                            output.accept(DI_BOX);
                            output.accept(GRAPHIC_EQ);
                            output.accept(COMPRESSOR);
                            output.accept(AMPLIFIER);
                            output.accept(FLOOR_WEDGE);
                            output.accept(SUBWOOFER);
                            output.accept(CONCERT_SPEAKER);
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
