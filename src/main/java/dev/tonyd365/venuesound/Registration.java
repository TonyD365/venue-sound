package dev.tonyd365.venuesound;

import dev.tonyd365.venuesound.block.MixerBlock;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.ReceiverBlock;
import dev.tonyd365.venuesound.block.ReceiverBlockEntity;
import dev.tonyd365.venuesound.block.SpeakerBlock;
import dev.tonyd365.venuesound.block.SpeakerBlockEntity;
import dev.tonyd365.venuesound.item.AudioCableItem;
import dev.tonyd365.venuesound.item.WirelessMicItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
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
    public static final MixerBlock MIXER = new MixerBlock(deviceProps());
    public static final SpeakerBlock SPEAKER = new SpeakerBlock(deviceProps());

    public static final WirelessMicItem WIRELESS_MIC = new WirelessMicItem(new Item.Properties().stacksTo(1));
    public static final AudioCableItem AUDIO_CABLE = new AudioCableItem(new Item.Properties().stacksTo(1));

    public static BlockEntityType<ReceiverBlockEntity> RECEIVER_BE;
    public static BlockEntityType<MixerBlockEntity> MIXER_BE;
    public static BlockEntityType<SpeakerBlockEntity> SPEAKER_BE;

    public static void register() {
        block("receiver", RECEIVER);
        block("mixer", MIXER);
        block("speaker", SPEAKER);

        Registry.register(BuiltInRegistries.ITEM, VenueSound.id("wireless_mic"), WIRELESS_MIC);
        Registry.register(BuiltInRegistries.ITEM, VenueSound.id("audio_cable"), AUDIO_CABLE);

        RECEIVER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, VenueSound.id("receiver"),
                FabricBlockEntityTypeBuilder.create(ReceiverBlockEntity::new, RECEIVER).build());
        MIXER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, VenueSound.id("mixer"),
                FabricBlockEntityTypeBuilder.create(MixerBlockEntity::new, MIXER).build());
        SPEAKER_BE = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, VenueSound.id("speaker"),
                FabricBlockEntityTypeBuilder.create(SpeakerBlockEntity::new, SPEAKER).build());

        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, VenueSound.id("main"),
                FabricItemGroup.builder()
                        .title(Component.translatable("itemGroup.venuesound.main"))
                        .icon(() -> new ItemStack(MIXER))
                        .displayItems((params, output) -> {
                            output.accept(WIRELESS_MIC);
                            output.accept(RECEIVER);
                            output.accept(MIXER);
                            output.accept(SPEAKER);
                            output.accept(AUDIO_CABLE);
                        })
                        .build());
    }

    private static void block(String name, Block block) {
        Registry.register(BuiltInRegistries.BLOCK, VenueSound.id(name), block);
        Registry.register(BuiltInRegistries.ITEM, VenueSound.id(name), new BlockItem(block, new Item.Properties()));
    }
}
