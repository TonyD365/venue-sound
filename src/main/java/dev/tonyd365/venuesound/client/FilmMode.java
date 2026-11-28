package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
import dev.tonyd365.venuesound.block.CableType;
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
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.util.List;

/**
 * Development film mode (only with -Dvenuesound.film=true, `./gradlew runClient -Pfilm=true`):
 * builds a small venue (stage, PA, front-of-house desk, rack), then plays a scripted camera
 * path while cables get plugged in and faders move, saving one frame per tick (20 fps) to
 * run/screenshots/film/f00000.png... tools/make_video.py adds titles, subtitles and music.
 */
public final class FilmMode {

    /** One camera move: from a to b over the given ticks, looking at a point that also moves. */
    private record Move(int ticks, Vec3 from, Vec3 to, Vec3 lookFrom, Vec3 lookTo) {
    }

    private static int tick;
    private static int stage;
    private static int frame;
    private static volatile boolean built;
    private static int y;
    private static BlockPos console, amp, receiver, standMic, paLeft, paRight, iem, intercom, wedge;

    private FilmMode() {
    }

    public static void register() {
        if (!Boolean.getBoolean("venuesound.film")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(FilmMode::tick);
    }

    // ------------------------------------------------------------------ script

    /** The shots, in ticks (20 per second); tools/make_video.py's subtitles follow the same times. */
    private static List<Move> moves() {
        Vec3 stageC = v(0, 2, -8), desk = v(0, 1, 6);
        return List.of(
                // 0-100: arrive over the venue
                new Move(100, v(10, 18, 30), v(4, 11, 20), stageC, v(0, 1, -2)),
                // 100-200: the wireless receiver; its cable is plugged in at 140
                new Move(100, v(10.5, 2.6, 10.2), v(9.2, 2.2, 8.8), v(7.5, 1.2, 6.5), v(7.5, 1.1, 6.5)),
                // 200-320: along the cables from the desk to the PA
                new Move(120, v(6, 4, 11), v(-12, 5, 2), desk, v(-9, 2, -8)),
                // 320-500: the console up close, faders moving, meters live
                new Move(180, v(-1.6, 3.4, 9.2), v(2.4, 3.2, 9.0), v(0.6, 1.4, 6.6), v(1.8, 1.4, 6.6)),
                // 500-600: the rear sockets, facing the stage
                new Move(100, v(-1.5, 2.2, 3.4), v(3.5, 2.0, 3.6), v(0.5, 1.2, 5.2), v(2.0, 1.2, 5.2)),
                // 600-720: the rack
                new Move(120, v(-10, 2.6, 13.5), v(-1, 2.4, 13.5), v(-9, 1.2, 10.5), v(-1, 1.2, 10.5)),
                // 720-820: monitors and intercom on stage
                new Move(100, v(4, 3.2, -2.5), v(-2, 2.8, -3.0), v(1, 1.6, -6.5), v(-1, 1.5, -6.5)),
                // 820-940: the desk again, run from outside (OSC): faders move by themselves
                new Move(120, v(3.5, 4.4, 11), v(-2.5, 4.4, 11), v(0.8, 1.2, 6.4), v(0.8, 1.2, 6.4)),
                // 940-1040: pull back over the whole venue
                new Move(100, v(4, 9, 18), v(14, 22, 34), v(0, 1, -2), stageC));
    }

    private static Vec3 v(double x, double yy, double z) {
        return new Vec3(x, y + yy, z);
    }

    // ------------------------------------------------------------------ tick

    private static void tick(Minecraft mc) {
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (stage == 0 && mc.screen instanceof TitleScreen && tick > 40) {
            stage = 1;
            LevelSettings settings = new LevelSettings("venuesound-film", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                    true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("venuesound-film-" + System.currentTimeMillis(), settings,
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
        if (stage == 2) {
            if (built && tick > 80) {
                stage = 3;
                tick = 0;
                new File(mc.gameDirectory, "screenshots/film").mkdirs();
                mc.options.hideGui = true;
                mc.options.bobView().set(false);
                mc.options.fov().set(55);
                mc.player.getAbilities().flying = true;
            }
            return;
        }
        if (stage != 3) {
            return;
        }
        // camera
        int t = tick, start = 0;
        Move m = null;
        for (Move mv : moves()) {
            if (t < start + mv.ticks()) {
                m = mv;
                break;
            }
            start += mv.ticks();
        }
        if (m == null) {
            stage = 4;
            VenueSound.LOGGER.info("film: {} frames saved", frame);
            mc.stop();
            return;
        }
        double k = ease((t - start) / (double) (m.ticks() - 1));
        Vec3 cam = lerp(m.from(), m.to(), k), look = lerp(m.lookFrom(), m.lookTo(), k);
        Vec3 d = look.subtract(cam);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) Math.toDegrees(Math.atan2(-d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
        if (t == start) {
            Vec3 c = cam;
            mc.getSingleplayerServer().execute(() -> {
                var sp = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
                sp.getAbilities().flying = true;
                sp.onUpdateAbilities();
                sp.teleportTo(sp.serverLevel(), c.x, c.y - 1.62, c.z, yaw, pitch);
            });
        }
        mc.player.getAbilities().flying = true;
        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.moveTo(cam.x, cam.y - 1.62, cam.z, yaw, pitch);
        mc.player.setOldPosAndRot();
        mc.player.yHeadRot = yaw;
        mc.player.yHeadRotO = yaw;

        events(mc, t);
        if (t >= 2) { // the first frames of a run can still be loading
            Screenshot.grab(mc.gameDirectory, String.format("film/f%05d.png", frame++), mc.getMainRenderTarget(), msg -> { });
        }
    }

    private static double ease(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double k) {
        return a.add(b.subtract(a).scale(k));
    }

    /** What happens in the venue at each tick: cables plugged in, faders moving, meters. */
    private static void events(Minecraft mc, int t) {
        var server = mc.getSingleplayerServer();
        if (t == 140) {
            server.execute(() -> link(server.overworld(), receiver, 0, console, 0, CableType.XLR, true));
        }
        if (t == 150) {
            server.execute(() -> link(server.overworld(), standMic, 0, console, 1, CableType.XLR, true));
        }
        if (t == 215) {
            server.execute(() -> {
                link(server.overworld(), console, 0, amp, 0, CableType.XLR, false);
                link(server.overworld(), console, 1, amp, 1, CableType.XLR, false);
            });
        }
        if (t == 240) {
            server.execute(() -> {
                link(server.overworld(), amp, 0, paLeft, 0, CableType.SPEAKER, false);
                link(server.overworld(), amp, 2, paRight, 0, CableType.SPEAKER, false);
            });
        }
        if (t == 260) {
            server.execute(() -> link(server.overworld(), console, 3, iem, 0, CableType.AUDIO, false));
        }
        // faders: a slow ride on the console close-up, then "OSC" moving several at once
        if ((t >= 320 && t < 500) || (t >= 820 && t < 940)) {
            double ph = (t - 320) / 20.0;
            server.execute(() -> {
                if (server.overworld().getBlockEntity(console) instanceof MixerBlockEntity mx) {
                    mx.set("fader.0", (float) (1.0 + 0.6 * Math.sin(ph * 0.9)));
                    mx.set("fader.1", (float) (0.9 + 0.5 * Math.sin(ph * 0.7 + 1)));
                    mx.set("high.0", (float) Math.round(6 * Math.sin(ph * 0.5)) * 2F);
                    mx.set("aux1.0", (float) (0.5 + 0.5 * Math.sin(ph * 0.6)));
                    if (t >= 820) {
                        for (int s = 2; s < 8; s++) {
                            mx.set("fader." + s, (float) (1.0 + 0.7 * Math.sin(ph * 0.8 + s)));
                        }
                        mx.set("main.l", (float) (1.1 + 0.3 * Math.sin(ph * 0.5)));
                        mx.set("main.r", (float) (1.1 + 0.3 * Math.sin(ph * 0.5)));
                    }
                }
            });
        }
        // live meters: a singing voice on channel 1 and the mix on MAIN
        if (t >= 150) {
            double s = Math.abs(Math.sin(t * 0.37)) * 0.6 + Math.abs(Math.sin(t * 0.11)) * 0.35;
            float main = (float) Math.min(1, 0.15 + s * 0.55);
            float[] levels = new float[22];
            levels[0] = main;
            levels[1] = (float) Math.min(1, main * (0.9 + 0.1 * Math.sin(t * 0.5)));
            levels[2] = (float) Math.min(1, 0.3 + s * 0.6);
            levels[3] = (float) (0.2 + s * 0.4);
            levels[2] = t % 47 < 3 ? 0.95F : levels[2]; // an occasional PEAK
            ConsoleMeters.update(console, levels);
        }
    }

    // ------------------------------------------------------------------ the venue

    private static void build(ServerLevel level) {
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        // stage: a raised floor with a dark back wall
        for (int x = -7; x <= 7; x++) {
            for (int z = -11; z <= -5; z++) {
                level.setBlock(new BlockPos(x, y, z), Blocks.SMOOTH_QUARTZ.defaultBlockState(), 3);
            }
            for (int h = 1; h <= 5; h++) {
                level.setBlock(new BlockPos(x, y + h - 1, -12), Blocks.BLACK_CONCRETE.defaultBlockState(), 3);
            }
        }
        // front-of-house platform
        for (int x = -11; x <= 9; x++) {
            for (int z = 4; z <= 12; z++) {
                level.setBlock(new BlockPos(x, y - 1, z), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
            }
        }
        console = new BlockPos(0, y, 6);
        place(level, Registration.MIXER, console, Direction.SOUTH);
        amp = new BlockPos(5, y, 6);
        place(level, Registration.AMPLIFIER, amp, Direction.SOUTH);
        receiver = new BlockPos(7, y, 6);
        place(level, Registration.RECEIVER, receiver, Direction.SOUTH);
        intercom = new BlockPos(-4, y, 6);
        place(level, Registration.INTERCOM_STATION, intercom, Direction.SOUTH);
        Block[] rack = {Registration.GRAPHIC_EQ, Registration.COMPRESSOR, Registration.FX_PROCESSOR, Registration.CROSSOVER,
                Registration.POWER_SEQUENCER, Registration.RACK_CABINET, Registration.DI_BOX};
        for (int i = 0; i < rack.length; i++) {
            place(level, rack[i], new BlockPos(-9 + i * 1, y, 10), Direction.SOUTH);
        }
        // PA left and right of the stage, facing the audience
        paLeft = new BlockPos(-9, y, -7);
        place(level, Registration.PA_SPEAKER, paLeft, Direction.SOUTH);
        paRight = new BlockPos(9, y, -7);
        place(level, Registration.PA_SPEAKER, paRight, Direction.SOUTH);
        place(level, Registration.SUBWOOFER, new BlockPos(-9, y, -5), Direction.SOUTH);
        place(level, Registration.SUBWOOFER, new BlockPos(9, y, -5), Direction.SOUTH);
        // on stage: a mic, a wedge facing the singer, in-ear transmitter
        standMic = new BlockPos(0, y + 1, -8);
        place(level, Registration.STAND_MIC, standMic, Direction.SOUTH);
        wedge = new BlockPos(0, y + 1, -6);
        place(level, Registration.FLOOR_WEDGE, wedge, Direction.NORTH);
        iem = new BlockPos(-3, y + 1, -9);
        place(level, Registration.IEM_TRANSMITTER, iem, Direction.SOUTH);
        place(level, Registration.STAGE_BOX, new BlockPos(3, y + 1, -10), Direction.SOUTH);
        built = true;
    }

    private static void place(ServerLevel level, Block block, BlockPos pos, Direction facing) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(DeviceBlock.FACING)) {
            state = state.setValue(DeviceBlock.FACING, facing);
        }
        level.setBlock(pos, state, 3);
        block.setPlacedBy(level, pos, state, null, ItemStack.EMPTY);
    }

    private static void link(ServerLevel level, BlockPos from, int fromPort, BlockPos to, int toPort, CableType type, boolean mixerInput) {
        if (!(level.getBlockEntity(from) instanceof AudioNodeBlockEntity src)) {
            return;
        }
        if (mixerInput && level.getBlockEntity(to) instanceof MixerBlockEntity mx && !mx.assignInput(from, toPort)) {
            return;
        }
        src.addLink(to, fromPort, toPort, List.of(), type);
    }
}
