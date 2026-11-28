package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
import dev.tonyd365.venuesound.block.DeviceBlock;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.List;

/**
 * Development self-test (only with -Dvenuesound.selftest=true): creates a flat world, places
 * devices, takes screenshots from a few angles into run/screenshots, then quits.
 */
public final class SelfTest {

    private static int tick;
    private static int stage;
    /** Height of the ground the devices stand on; cameras are given relative to y = 4. */
    private static volatile int baseY = 4;

    /** Camera views: x, y, z, yaw, pitch, screenshot name. */
    private record View(double x, double y, double z, float yaw, float pitch, String name) {
    }

    private static final List<View> VIEWS = List.of(
            new View(4.5, 7.0, 3.8, 180, 55, "selftest-console.png"),
            new View(4.5, 6.0, 1.2, 180, 82, "selftest-console-close.png"),
            new View(4.5, 5.0, 3.0, 180, 18, "selftest-console-front.png"),
            new View(4.5, 5.6, -3.4, 0, 22, "selftest-console-rear.png"),
            new View(4.23, 5.9, 0.62, 180, 89, "selftest-hud-fader.png"),
            new View(4.23, 5.9, 0.11, 180, 89, "selftest-hud-knob.png"),
            new View(8.5, 5.2, 2.2, 180, 22, "selftest-amp-front.png"),
            new View(8.5, 5.2, -1.8, 0, 22, "selftest-amp-back.png"),
            new View(13.5, 5.0, 1.7, 180, 35, "selftest-receiver.png"),
            new View(8.0, 6.5, 11.0, 180, 20, "selftest-pro-left.png"),
            new View(22.0, 6.5, 11.0, 180, 20, "selftest-pro-right.png"),
            new View(21.5, 5.4, 1.6, 0, 32, "selftest-pro-back.png"),
            new View(21.5, 5.2, 6.6, 180, 28, "selftest-pro-front.png"),
            new View(5.5, 9.5, 15.5, 180, 50, "selftest-vs24.png"));

    private SelfTest() {
    }

    public static void register() {
        if (!Boolean.getBoolean("venuesound.selftest")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(SelfTest::tick);
    }

    private static void tick(Minecraft mc) {
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (stage == 0 && mc.screen instanceof TitleScreen && tick > 40) {
            stage = 1;
            LevelSettings settings = new LevelSettings("venuesound-selftest", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                    true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("venuesound-selftest-" + System.currentTimeMillis(), settings,
                    new WorldOptions(1L, false, false),
                    reg -> reg.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
            return;
        }
        if (stage == 1 && mc.level != null && mc.getSingleplayerServer() != null && mc.player != null) {
            stage = 2;
            tick = 0;
            mc.getSingleplayerServer().execute(() -> build(mc.getSingleplayerServer().overworld()));
            return;
        }
        if (stage >= 2 && stage - 2 < VIEWS.size()) {
            View v = VIEWS.get(stage - 2);
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            if (tick == 1 || tick == 30) {
                mc.getSingleplayerServer().execute(() -> {
                    sp.getAbilities().flying = true;
                    sp.onUpdateAbilities();
                    sp.teleportTo(sp.serverLevel(), v.x(), v.y() - 4 + baseY - 1.62, v.z(), v.yaw(), v.pitch());
                });
                // views named "...hud..." keep the interface, to show the control readout
                mc.options.hideGui = !v.name().contains("hud");
            }
            if (tick == 60) {
                Screenshot.grab(mc.gameDirectory, v.name(), mc.getMainRenderTarget(), msg -> VenueSound.LOGGER.info("selftest: {}", msg.getString()));
                stage++;
                tick = 0;
            }
            return;
        }
        if (stage - 2 == VIEWS.size() && tick > 20) {
            mc.stop();
        }
    }

    private static void place(ServerLevel level, Block block, BlockPos pos, Direction facing) {
        BlockState state = block.defaultBlockState().setValue(DeviceBlock.FACING, facing);
        level.setBlock(pos, state, 3);
        block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
    }

    /** Ground is at y = 4 on a default flat world (blocks sit at y 4). */
    private static void build(ServerLevel level) {
        level.setDayTime(6000);
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0);
        baseY = y;
        // console 3x2 facing south (operator stands south of it, at +z)
        BlockPos console = new BlockPos(3, y, 0);
        place(level, Registration.MIXER, console, Direction.SOUTH);
        BlockPos amp = new BlockPos(8, y, 0);
        place(level, Registration.AMPLIFIER, amp, Direction.SOUTH);
        BlockPos rx = new BlockPos(13, y, 0);
        place(level, Registration.RECEIVER, rx, Direction.SOUTH);
        BlockPos spk = new BlockPos(16, y, 0);
        place(level, Registration.SPEAKER, spk, Direction.SOUTH);
        if (level.getBlockEntity(console) instanceof MixerBlockEntity m) {
            m.set("fader.0", 1.6F);
            m.set("fader.2", 0.4F);
            m.set("mute.3", 1F);
            m.set("gain.1", 1.8F);
            m.set("main.l", 1.2F);
        }
        if (level.getBlockEntity(rx) instanceof AudioNodeBlockEntity r && level.getBlockEntity(console) instanceof MixerBlockEntity m) {
            if (m.assignInput(rx, 0)) {
                r.addLink(console, 0, 0, java.util.List.of(), dev.tonyd365.venuesound.block.CableType.XLR);
            }
        }
        if (level.getBlockEntity(console) instanceof AudioNodeBlockEntity c) {
            c.addLink(amp, 0, 0);
        }
        if (level.getBlockEntity(amp) instanceof AudioNodeBlockEntity a) {
            a.addLink(spk, 0, 0, java.util.List.of(), dev.tonyd365.venuesound.block.CableType.SPEAKER);
        }
        place(level, Registration.MIXER24, new BlockPos(3, y, 10), Direction.SOUTH);
        // pro devices in a row behind the console
        net.minecraft.world.level.block.Block[] pro = {Registration.GRAPHIC_EQ, Registration.COMPRESSOR, Registration.FX_PROCESSOR,
                Registration.CROSSOVER, Registration.DI_BOX, Registration.POWER_SEQUENCER, Registration.STAGE_BOX, Registration.STAGE_BOX_16,
                Registration.VIDEO_CAPTURE, Registration.IEM_TRANSMITTER, Registration.INTERCOM_STATION, Registration.FLOOR_WEDGE, Registration.SUBWOOFER,
                Registration.GOOSENECK_MIC, Registration.RACK_CABINET, Registration.TRUSS_POINT};
        for (int i = 0; i < pro.length; i++) {
            place(level, pro[i], new BlockPos(1 + i * 2, y, 4), Direction.SOUTH);
        }
        VenueSound.LOGGER.info("selftest: devices placed");
    }
}
