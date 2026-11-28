package dev.tonyd365.venuesound.item;

import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
import dev.tonyd365.venuesound.block.CableClipBlock;
import dev.tonyd365.venuesound.block.CableRoute;
import dev.tonyd365.venuesound.block.DeviceBlock;
import dev.tonyd365.venuesound.block.MixerBlock;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.PaSpeakerBlock;
import dev.tonyd365.venuesound.block.PanelMath;
import dev.tonyd365.venuesound.block.PortLayout;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Audio cable, plugged into the sockets on the devices: right-click an OUT socket, then an IN
 * socket. Sneak + right-click a socket to unplug the cable leaving it; sneak + right-click the
 * air to drop the held end.
 */
public class AudioCableItem extends Item {

    /** Longest single cable run, in blocks. Use outlets to go further. */
    public static final int MAX_LENGTH = dev.tonyd365.venuesound.block.CableType.AUDIO.maxLength;

    public AudioCableItem(Properties properties) {
        super(properties);
    }

    /** Which kind of cable this item is (audio, XLR, speaker, snake, reel). */
    public dev.tonyd365.venuesound.block.CableType cableType() {
        return dev.tonyd365.venuesound.block.CableType.byId(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this).getPath());
    }

    public static dev.tonyd365.venuesound.block.CableType typeOf(ItemStack stack) {
        return stack.getItem() instanceof AudioCableItem c ? c.cableType() : dev.tonyd365.venuesound.block.CableType.AUDIO;
    }

    /** Resolves multi-block devices (PA cabinet top, console right half) to the block holding the device. */
    public static BlockPos devicePos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PaSpeakerBlock) {
            return PaSpeakerBlock.basePos(state, pos);
        }
        if (state.getBlock() instanceof MixerBlock) {
            return MixerBlock.mainPos(state, pos);
        }
        if (state.getBlock() instanceof dev.tonyd365.venuesound.block.ConcertSpeakerBlock) {
            return dev.tonyd365.venuesound.block.ConcertSpeakerBlock.mainPos(state, pos);
        }
        return pos;
    }

    /** The socket clicked on a device, or null if the click missed every socket. */
    @Nullable
    public static PortLayout.Port portAt(Level level, BlockPos device, String key, Direction clickedFace, net.minecraft.world.phys.Vec3 hit) {
        BlockState state = level.getBlockState(device);
        if (!state.hasProperty(DeviceBlock.FACING)) {
            return null;
        }
        Direction facing = state.getValue(DeviceBlock.FACING);
        PanelMath.Point p = PanelMath.local(device, facing, hit);
        PortLayout.Face face = clickedFace == facing ? PortLayout.Face.FRONT
                : clickedFace == facing.getOpposite() ? PortLayout.Face.BACK : null;
        if (face == null) {
            // the stand mic accepts a click anywhere
            return PortLayout.at(key, PortLayout.Face.ANY, p.u(), p.height());
        }
        return PortLayout.at(key, face, p.u(), p.height());
    }

    /** World position of a socket on the device at {@code device}. */
    public static net.minecraft.world.phys.Vec3 socketWorld(Level level, BlockPos device, String key, PortLayout.Dir dir, int index) {
        BlockState state = level.getBlockState(device);
        if (!state.hasProperty(DeviceBlock.FACING)) {
            return net.minecraft.world.phys.Vec3.atCenterOf(device);
        }
        return PanelMath.socket(state.getValue(DeviceBlock.FACING), PortLayout.byIndex(key, dir, index)).add(device.getX(), device.getY(), device.getZ());
    }

    /** Where the held cable currently ends: the last clip it runs through, or the socket it is plugged into. */
    @Nullable
    private static net.minecraft.world.phys.Vec3 heldEnd(Level level, CompoundTag tag) {
        long[] via = tag.getLongArray("Via");
        if (via.length > 0) {
            BlockPos clip = BlockPos.of(via[via.length - 1]);
            BlockState cs = level.getBlockState(clip);
            return cs.getBlock() instanceof CableClipBlock ? CableClipBlock.anchor(cs, clip) : null;
        }
        BlockPos from = BlockPos.of(tag.getLong("From"));
        if (!(level.getBlockEntity(from) instanceof AudioNodeBlockEntity src)) {
            return null;
        }
        return socketWorld(level, from, src.deviceKey(), PortLayout.Dir.OUT, tag.getInt("FromPort"));
    }

    /** Runs the held cable through a clip. */
    private static InteractionResult useClip(Level level, Player player, ItemStack stack, BlockPos clip) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("From")) {
            say(player, Component.translatable("message.venuesound.clip_need_cable").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }
        net.minecraft.world.phys.Vec3 end = heldEnd(level, tag);
        net.minecraft.world.phys.Vec3 here = CableClipBlock.anchor(level.getBlockState(clip), clip);
        if (end == null) {
            clearHeld(stack);
            say(player, Component.translatable("message.venuesound.cable_source_gone").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        int maxLength = typeOf(stack).maxLength;
        if (end.distanceTo(here) > maxLength) {
            say(player, Component.translatable("message.venuesound.cable_too_far", maxLength).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (!CableRoute.clear(level, end, here)) {
            say(player, Component.translatable("message.venuesound.cable_blocked").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        long[] via = tag.getLongArray("Via");
        long[] more = java.util.Arrays.copyOf(via, via.length + 1);
        more[via.length] = clip.asLong();
        tag.putLongArray("Via", more);
        click(level, clip);
        say(player, Component.translatable("message.venuesound.clip_added", more.length));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.getBlockState(context.getClickedPos()).getBlock() instanceof CableClipBlock) {
            return level.isClientSide || player == null ? InteractionResult.SUCCESS
                    : useClip(level, player, context.getItemInHand(), context.getClickedPos());
        }
        BlockPos clicked = devicePos(level, context.getClickedPos());
        if (!(level.getBlockEntity(clicked) instanceof AudioNodeBlockEntity target)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        PortLayout.Port port = portAt(level, clicked, target.deviceKey(), context.getClickedFace(), context.getClickLocation());
        if (port == null) {
            say(player, Component.translatable("message.venuesound.cable_click_socket").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            if (port.givesOut()) {
                target.unplugPort(port.index());
            }
            if (target instanceof MixerBlockEntity mixer && port.dir() == PortLayout.Dir.IN) {
                BlockPos upstream = mixer.getInput(port.index());
                if (upstream != null && level.getBlockEntity(upstream) instanceof AudioNodeBlockEntity src) {
                    for (AudioNodeBlockEntity.Link l : src.getLinks()) {
                        if (l.target().equals(clicked)) {
                            src.unplugPort(l.fromPort());
                        }
                    }
                }
                mixer.releaseInputSlot(port.index());
            }
            clearHeld(stack);
            say(player, Component.translatable("message.venuesound.cable_unplugged_port", port.label()));
            return InteractionResult.SUCCESS;
        }

        CompoundTag tag = stack.getTag();
        boolean holding = tag != null && tag.contains("From")
                && level.dimension().location().toString().equals(tag.getString("FromDim"));
        if (!holding) {
            // First click: plug one end into an OUT socket.
            if (!port.givesOut()) {
                say(player, Component.translatable("message.venuesound.cable_start_out", port.label()).withStyle(ChatFormatting.YELLOW));
            } else if (target.portInUse(port.index())) {
                say(player, Component.translatable("message.venuesound.cable_port_busy", port.label()).withStyle(ChatFormatting.YELLOW));
            } else if (!typeOf(stack).fits(PortLayout.connector(target.deviceKey(), port))) {
                say(player, wrongPlug(port, PortLayout.connector(target.deviceKey(), port), typeOf(stack)));
            } else {
                CompoundTag t = stack.getOrCreateTag();
                t.putLong("From", clicked.asLong());
                t.putString("FromDim", level.dimension().location().toString());
                t.putInt("FromPort", port.index());
                t.putString("FromLabel", port.label());
                say(player, Component.translatable("message.venuesound.cable_from_port", port.label()));
                click(level, clicked);
            }
            return InteractionResult.SUCCESS;
        }

        // Second click: plug the other end into an IN socket.
        BlockPos from = BlockPos.of(tag.getLong("From"));
        int fromPort = tag.getInt("FromPort");
        java.util.List<BlockPos> via = new java.util.ArrayList<>();
        for (long v : tag.getLongArray("Via")) {
            via.add(BlockPos.of(v));
        }
        net.minecraft.world.phys.Vec3 end = heldEnd(level, tag);
        net.minecraft.world.phys.Vec3 into = socketWorld(level, clicked, target.deviceKey(), PortLayout.Dir.IN, port.index());
        dev.tonyd365.venuesound.block.CableType type = typeOf(stack);
        if (end != null && end.distanceTo(into) <= type.maxLength && !CableRoute.clear(level, end, into)) {
            say(player, Component.translatable("message.venuesound.cable_blocked").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        clearHeld(stack);
        if (from.equals(clicked)) {
            say(player, Component.translatable("message.venuesound.cable_cleared"));
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(from) instanceof AudioNodeBlockEntity source)) {
            say(player, Component.translatable("message.venuesound.cable_source_gone").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (!port.acceptsIn()) {
            say(player, Component.translatable("message.venuesound.cable_end_in", port.label()).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (end == null || end.distanceTo(into) > type.maxLength) {
            say(player, Component.translatable("message.venuesound.cable_too_far", type.maxLength).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (!source.kind().canFeed(target.kind())) {
            say(player, Component.translatable("message.venuesound.cable_wrong_way").withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        PortLayout.Connector socket = PortLayout.connector(target.deviceKey(), port);
        if (!type.fits(socket)) {
            say(player, wrongPlug(port, socket, type));
            return InteractionResult.SUCCESS;
        }
        String refused = type.refuseTarget(target.kind());
        if (refused != null) {
            say(player, Component.translatable(refused).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (source.hasLink(clicked, port.index())) {
            say(player, Component.translatable("message.venuesound.cable_already").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }
        if (source.portInUse(fromPort) || !source.canAddOutput()) {
            say(player, Component.translatable("message.venuesound.cable_outputs_full").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS;
        }
        if (target instanceof MixerBlockEntity mixer && !mixer.assignInput(from, port.index())) {
            say(player, Component.translatable("message.venuesound.cable_port_busy", port.label()).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        source.addLink(clicked, fromPort, port.index(), via, type);
        click(level, clicked);
        say(player, Component.translatable("message.venuesound.cable_connected_ports", tag.getString("FromLabel"), port.label()).withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }

    /** "IN 3 is an XLR/TRS socket: this cable has TRS plugs." plus which cable would fit. */
    private static Component wrongPlug(PortLayout.Port port, PortLayout.Connector socket, dev.tonyd365.venuesound.block.CableType cable) {
        String fits = switch (socket) {
            case XLR -> "item.venuesound.xlr_cable";
            case TRS -> "item.venuesound.audio_cable";
            case COMBO -> "item.venuesound.xlr_cable";
            case SPEAKON -> "item.venuesound.speaker_cable";
            case MULTIPIN -> "item.venuesound.snake_cable";
            case ANY -> "item.venuesound.audio_cable";
        };
        return Component.translatable("message.venuesound.cable_wrong_plug", port.label(), socket.label, cable.plug.label,
                Component.translatable(fits)).withStyle(ChatFormatting.RED);
    }

    private static void click(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.5F, 1.4F);
    }

    private static void clearHeld(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove("From");
            tag.remove("FromDim");
            tag.remove("FromPort");
            tag.remove("FromLabel");
            tag.remove("Via");
        }
    }

    private static void say(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && stack.getTag() != null && stack.getTag().contains("From")) {
            if (!level.isClientSide) {
                clearHeld(stack);
                say(player, Component.translatable("message.venuesound.cable_cleared"));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.getTag() != null && stack.getTag().contains("From");
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        dev.tonyd365.venuesound.block.CableType type = cableType();
        tooltip.add(Component.translatable("tooltip.venuesound.cable." + type.id, type.maxLength).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.venuesound.cable_help").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.venuesound.cable_help2", type.maxLength).withStyle(ChatFormatting.DARK_GRAY));
    }
}
