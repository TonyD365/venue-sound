package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.NodeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Stores the capture zone (two corners, set with the Zone Selector). Acts as a source node. */
public class VideoCaptureBlockEntity extends AudioNodeBlockEntity {

    /** Capture blocks loaded on this client, for the WATERFrAMES volume hook. */
    public static final Set<VideoCaptureBlockEntity> CLIENT = ConcurrentHashMap.newKeySet();

    @Nullable
    private BlockPos a, b;

    public VideoCaptureBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.VIDEO_CAPTURE_BE, pos, state);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.MEDIA_DECK;
    }

    @Override
    public String deviceKey() {
        return "video_capture";
    }

    public void setZone(BlockPos a, BlockPos b) {
        this.a = a.immutable();
        this.b = b.immutable();
        changed();
    }

    @Nullable
    public AABB zone() {
        return a == null || b == null ? null : new AABB(a).minmax(new AABB(b));
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level.isClientSide) {
            CLIENT.add(this);
        }
    }

    @Override
    public void setRemoved() {
        CLIENT.remove(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (a != null && b != null) {
            tag.putLong("ZoneA", a.asLong());
            tag.putLong("ZoneB", b.asLong());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        a = tag.contains("ZoneA") ? BlockPos.of(tag.getLong("ZoneA")) : null;
        b = tag.contains("ZoneB") ? BlockPos.of(tag.getLong("ZoneB")) : null;
    }
}
