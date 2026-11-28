package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.VenueSound;
import dev.tonyd365.venuesound.block.DeviceBlock;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Development photo shoot (only with -Dvenuesound.photos=true, `./gradlew runClient -Pphotos=true`):
 * puts every VenueSound item alone in a flat world and saves, into run/screenshots,
 * photo-&lt;id&gt;.png (the item in the world, seen from the front at three-quarters) and
 * icon-&lt;id&gt;.png (its inventory icon, large, on a magenta background for tools/make_item_images.py
 * to cut out). Then quits.
 */
public final class PhotoShoot {

    private static final int SPACING = 24;
    private static final int ICON_BG = 0xFFFF00FF;

    private record Shot(String id, ItemStack stack, Vec3 camera, float yaw, float pitch) {
    }

    private static final List<ItemStack> ITEMS = new ArrayList<>();
    private static final List<Shot> SHOTS = new ArrayList<>();
    private static int tick;
    private static int stage;
    private static volatile boolean built;

    private PhotoShoot() {
    }

    public static void register() {
        if (!Boolean.getBoolean("venuesound.photos")) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(PhotoShoot::tick);
    }

    private static void tick(Minecraft mc) {
        tick++;
        mc.options.pauseOnLostFocus = false;
        if (stage == 0 && mc.screen instanceof TitleScreen && tick > 40) {
            stage = 1;
            LevelSettings settings = new LevelSettings("venuesound-photos", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                    true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("venuesound-photos-" + System.currentTimeMillis(), settings,
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
        if (stage < 3) {
            if (stage == 2 && built && tick > 40) {
                stage = 3;
                tick = 0;
            }
            return;
        }
        int i = stage - 3;
        if (i < SHOTS.size()) {
            Shot s = SHOTS.get(i);
            ServerPlayer sp = mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            if (tick == 1 || tick == 30) {
                mc.getSingleplayerServer().execute(() -> {
                    sp.getAbilities().flying = true;
                    sp.onUpdateAbilities();
                    sp.teleportTo(sp.serverLevel(), s.camera().x, s.camera().y - 1.62, s.camera().z, s.yaw(), s.pitch());
                });
                mc.options.hideGui = true;
                // a long lens: the item fills the picture without wide-angle distortion
                mc.options.fov().set(30);
            }
            if (tick == 70) {
                Screenshot.grab(mc.gameDirectory, "photo-" + s.id() + ".png", mc.getMainRenderTarget(), msg -> { });
                stage++;
                tick = 0;
            }
            return;
        }
        int j = i - SHOTS.size();
        if (j < ITEMS.size()) {
            ItemStack stack = ITEMS.get(j);
            if (tick == 1) {
                mc.setScreen(new IconScreen(stack));
            }
            if (tick == 12) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
                Screenshot.grab(mc.gameDirectory, "icon-" + id + ".png", mc.getMainRenderTarget(), msg -> { });
                stage++;
                tick = 0;
            }
            return;
        }
        if (tick > 20) {
            VenueSound.LOGGER.info("photos: {} world photos and {} icons saved", SHOTS.size(), ITEMS.size());
            mc.stop();
        }
    }

    /** Places every item in its own spot and works out a camera for each. */
    private static void build(ServerLevel level) {
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(VenueSound.MOD_ID)) {
                ITEMS.add(new ItemStack(item));
            }
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        for (int n = 0; n < ITEMS.size(); n++) {
            ItemStack stack = ITEMS.get(n);
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
            BlockPos pos = new BlockPos(n * SPACING, y, 0);
            AABB box;
            if (stack.getItem() instanceof BlockItem bi) {
                box = placeBlock(level, bi.getBlock(), pos);
            } else {
                box = frame(level, stack, pos);
            }
            SHOTS.add(camera(id, stack, box));
        }
        built = true;
    }

    private static AABB placeBlock(ServerLevel level, Block block, BlockPos pos) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(DeviceBlock.FACING)) {
            state = state.setValue(DeviceBlock.FACING, Direction.SOUTH);
        } else if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, Direction.UP);
        } else if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        }
        level.setBlock(pos, state, 3);
        block.setPlacedBy(level, pos, state, null, new ItemStack(block));
        // the footprint: every non-air block near the spot (multi-block devices fill several)
        int x0 = pos.getX(), x1 = pos.getX(), y1 = pos.getY(), z0 = pos.getZ(), z1 = pos.getZ();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-8, 0, -8), pos.offset(8, 8, 8))) {
            if (!level.getBlockState(p).isAir()) {
                x0 = Math.min(x0, p.getX());
                x1 = Math.max(x1, p.getX());
                y1 = Math.max(y1, p.getY());
                z0 = Math.min(z0, p.getZ());
                z1 = Math.max(z1, p.getZ());
            }
        }
        return new AABB(x0, pos.getY(), z0, x1 + 1, y1 + 1, z1 + 1);
    }

    /** A non-block item: shown in an item frame on a small stand. */
    private static AABB frame(ServerLevel level, ItemStack stack, BlockPos pos) {
        level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        level.setBlock(pos.above(), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        ItemFrame frame = new ItemFrame(level, pos.above().south(), Direction.SOUTH);
        frame.setItem(stack.copy(), false);
        level.addFreshEntity(frame);
        return new AABB(pos.getX(), pos.getY() + 1, pos.getZ() + 1, pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1.1);
    }

    /** Front three-quarter view from the south-east, far enough to fit the whole box. */
    private static Shot camera(String id, ItemStack stack, AABB box) {
        Vec3 c = box.getCenter();
        double size = Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize()));
        double d = stack.getItem() instanceof BlockItem ? 2.4 + size * 1.9 : 2.6;
        Vec3 cam = c.add(d * 0.55, d * 0.5, d);
        double dx = c.x - cam.x, dy = c.y - cam.y, dz = c.z - cam.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, Math.sqrt(dx * dx + dz * dz)));
        return new Shot(id, stack, cam, yaw, pitch);
    }

    /** Draws one item's icon large on a flat background. */
    private static final class IconScreen extends Screen {
        private final ItemStack stack;

        IconScreen(ItemStack stack) {
            super(Component.empty());
            this.stack = stack;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, width, height, ICON_BG);
            float scale = Math.min(width, height) / 20F;
            g.pose().pushPose();
            g.pose().translate(width / 2F - 8 * scale, height / 2F - 8 * scale, 0);
            g.pose().scale(scale, scale, 1);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
