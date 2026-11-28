package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Mouse-wheel control of knobs and faders: finds the control under the crosshair (same tables as
 * clicks) and turns it one step per wheel notch. Used by the client to decide whether to swallow
 * the scroll, and by the server to apply it.
 */
public final class ScrollControl {

    private ScrollControl() {
    }

    /** The device holding the setting, and which setting. */
    public record Target(BlockPos device, String id) {
    }

    @Nullable
    public static Target find(Level level, BlockPos pos, Direction face, Vec3 hit) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof MixerBlock) {
            if (face != Direction.UP) {
                return null;
            }
            BlockPos main = MixerBlock.mainPos(state, pos);
            PanelMath.Point p = PanelMath.local(main, state.getValue(DeviceBlock.FACING), hit);
            MixerLayout.Control c = ((MixerBlock) state.getBlock()).layout().at(p.u(), p.depth());
            return c != null && (c.kind() == MixerLayout.Kind.KNOB || c.kind() == MixerLayout.Kind.FADER) ? new Target(main, c.param()) : null;
        }
        if (!state.hasProperty(DeviceBlock.FACING)) {
            return null;
        }
        Direction facing = state.getValue(DeviceBlock.FACING);
        PanelMath.Point p = PanelMath.local(pos, facing, hit);
        if (level.getBlockEntity(pos) instanceof AmplifierBlockEntity amp && face == facing) {
            ProcessorLayout.Ctl c = ProcessorLayout.at(amp.deviceKey(), p.u(), p.height());
            if (c != null) {
                return new Target(pos, c.param());
            }
            return PanelLayout.Amp.onKnob(p.u(), p.height()) ? new Target(pos, "amp.level") : null;
        }
        if (state.getBlock() instanceof SpeakerBlock && face == facing.getOpposite() && level.getBlockEntity(pos) instanceof SpeakerBlockEntity sp
                && PortLayout.SpeakerPanel.of(sp.deviceKey()).onKnob(p.u(), p.height())) {
            return new Target(pos, "speaker.range");
        }
        return null;
    }

    /** Turns the control one step: dir +1 up, -1 down. */
    public static void apply(Level level, Target t, int dir) {
        var be = level.getBlockEntity(t.device());
        if (be instanceof MixerBlockEntity m) {
            MixerLayout.Control c = m.layout().byParam(t.id());
            if (c == null) {
                return;
            }
            double step = c.kind() == MixerLayout.Kind.FADER ? 0.1 : c.step();
            double next = Math.round((m.get(c.param()) + dir * step) / step) * step;
            double min = c.kind() == MixerLayout.Kind.FADER ? 0 : c.min(), max = c.kind() == MixerLayout.Kind.FADER ? 2 : c.max();
            m.set(c.param(), (float) Math.max(min, Math.min(max, next)));
        } else if (be instanceof AmplifierBlockEntity amp) {
            if (t.id().equals("amp.level")) {
                amp.cycleLevel(dir < 0);
                return;
            }
            for (ProcessorLayout.Ctl c : ProcessorLayout.controls(amp.deviceKey())) {
                if (c.param().equals(t.id())) {
                    double next = Math.round((amp.get(c.param()) + dir * c.step()) / c.step()) * c.step();
                    amp.set(c.param(), (float) Math.max(c.min(), Math.min(c.max(), next)));
                }
            }
        } else if (be instanceof SpeakerBlockEntity s && t.id().equals("speaker.range")) {
            s.stepRange(dir < 0);
        }
    }
}
