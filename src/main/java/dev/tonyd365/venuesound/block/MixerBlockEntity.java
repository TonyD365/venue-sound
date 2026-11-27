package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.DimPos;
import dev.tonyd365.venuesound.audio.MediaPlayback;
import dev.tonyd365.venuesound.audio.NodeKind;
import dev.tonyd365.venuesound.audio.RouterState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mixing console: 10 input strips, master section and a built-in USB player (on strip ST1). */
public class MixerBlockEntity extends AudioNodeBlockEntity {

    public static final int INPUTS = MixerLayout.MAX_STRIPS;

    private final Map<String, Float> params = new HashMap<>();
    private final BlockPos[] inputs = new BlockPos[INPUTS];
    private String usbTrack = "";
    private boolean usbPlaying;

    public MixerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MIXER_BE, pos, state);
        params.putAll(layout().defaults());
    }

    public MixerLayout layout() {
        return getBlockState().getBlock() instanceof MixerBlock b ? b.layout() : MixerLayout.VS12;
    }

    @Override
    public String deviceKey() {
        return layout().key;
    }

    @Override
    public NodeKind kind() {
        return NodeKind.MIXER;
    }

    public float get(String param) {
        Float v = params.get(param);
        return v != null ? v : layout().defaults().getOrDefault(param, 0F);
    }

    public void set(String param, float value) {
        if (layout().defaults().containsKey(param)) {
            params.put(param, value);
            changed();
        }
    }

    public boolean isMuted(int strip) {
        return get("mute." + strip) > 0.5F;
    }

    @Nullable
    public BlockPos getInput(int input) {
        return inputs[input];
    }

    public String getUsbTrack() {
        return usbTrack;
    }

    public boolean isUsbPlaying() {
        return usbPlaying;
    }

    // ------------------------------------------------------------------ inputs

    /** Plugs {@code upstream} into input {@code slot}. False if another device is already plugged in there. */
    public boolean assignInput(BlockPos upstream, int slot) {
        if (slot < 0 || slot >= layout().strips || slot == layout().usbStrip || (inputs[slot] != null && !inputs[slot].equals(upstream))) {
            return false;
        }
        for (int i = 0; i < INPUTS; i++) {
            if (upstream.equals(inputs[i])) {
                inputs[i] = null;
            }
        }
        inputs[slot] = upstream.immutable();
        changed();
        return true;
    }

    public void releaseInput(BlockPos upstream) {
        for (int i = 0; i < INPUTS; i++) {
            if (upstream.equals(inputs[i])) {
                inputs[i] = null;
            }
        }
        changed();
    }

    public void releaseInputSlot(int slot) {
        if (slot >= 0 && slot < INPUTS && inputs[slot] != null) {
            inputs[slot] = null;
            changed();
        }
    }

    public void clearInputs() {
        for (int i = 0; i < INPUTS; i++) {
            inputs[i] = null;
        }
        changed();
    }

    // ------------------------------------------------------------------ USB player

    public void usbButton(String button) {
        List<String> tracks = MediaPlayback.listTracks();
        switch (button) {
            case "prev", "next" -> {
                if (tracks.isEmpty()) {
                    return;
                }
                int i = Math.max(0, tracks.indexOf(usbTrack));
                if (tracks.contains(usbTrack)) {
                    i = Math.floorMod(i + ("next".equals(button) ? 1 : -1), tracks.size());
                }
                usbTrack = tracks.get(i);
                if (usbPlaying) {
                    startUsb();
                }
                changed();
            }
            case "play" -> {
                if (usbTrack.isEmpty() && !tracks.isEmpty()) {
                    usbTrack = tracks.get(0);
                }
                if (!usbTrack.isEmpty()) {
                    usbPlaying = true;
                    startUsb();
                    changed();
                }
            }
            case "stop" -> {
                usbPlaying = false;
                if (level instanceof ServerLevel) {
                    MediaPlayback.get().stop(dimPos());
                }
                changed();
            }
            default -> {
            }
        }
    }

    private void startUsb() {
        if (level instanceof ServerLevel) {
            MediaPlayback.get().playIntoConsole(dimPos(), usbTrack, layout().usbStrip);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel) {
            MediaPlayback.get().stop(dimPos());
        }
        super.setRemoved();
    }

    // ------------------------------------------------------------------ router / persistence

    @Override
    protected RouterState.Node snapshot(ServerLevel level, List<RouterState.Out> outs) {
        DimPos[] in = new DimPos[INPUTS];
        for (int i = 0; i < INPUTS; i++) {
            in[i] = inputs[i] == null ? null : new DimPos(level.dimension(), inputs[i]);
        }
        return new RouterState.Node(dimPos(), kind(), outs, 0, new RouterState.Mixer(Map.copyOf(params), in), 0F, 0, 0.5D);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag p = new CompoundTag();
        params.forEach(p::putFloat);
        tag.put("Params", p);
        for (int i = 0; i < INPUTS; i++) {
            if (inputs[i] != null) {
                tag.putLong("Input" + i, inputs[i].asLong());
            }
        }
        tag.putString("UsbTrack", usbTrack);
        tag.putBoolean("UsbPlaying", usbPlaying);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        params.clear();
        params.putAll(layout().defaults());
        CompoundTag p = tag.getCompound("Params");
        for (String key : p.getAllKeys()) {
            if (layout().defaults().containsKey(key)) {
                params.put(key, p.getFloat(key));
            }
        }
        for (int i = 0; i < INPUTS; i++) {
            inputs[i] = tag.contains("Input" + i) ? BlockPos.of(tag.getLong("Input" + i)) : null;
        }
        usbTrack = tag.getString("UsbTrack");
        usbPlaying = tag.getBoolean("UsbPlaying");
    }
}
