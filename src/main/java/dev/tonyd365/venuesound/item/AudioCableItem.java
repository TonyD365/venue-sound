package dev.tonyd365.venuesound.item;

import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
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
    public static final int MAX_LENGTH = 32;

    public AudioCableItem(Properties properties) {
        super(properties);
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

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
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
        if (!from.closerThan(clicked, MAX_LENGTH)) {
            say(player, Component.translatable("message.venuesound.cable_too_far", MAX_LENGTH).withStyle(ChatFormatting.RED));
            return InteractionResult.SUCCESS;
        }
        if (!source.kind().canFeed(target.kind())) {
            say(player, Component.translatable("message.venuesound.cable_wrong_way").withStyle(ChatFormatting.RED));
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
        source.addLink(clicked, fromPort, port.index());
        click(level, clicked);
        say(player, Component.translatable("message.venuesound.cable_connected_ports", tag.getString("FromLabel"), port.label()).withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
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
        tooltip.add(Component.translatable("tooltip.venuesound.cable_help").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.venuesound.cable_help2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
