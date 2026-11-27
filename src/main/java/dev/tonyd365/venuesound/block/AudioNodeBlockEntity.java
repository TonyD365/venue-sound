package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.audio.DimPos;
import dev.tonyd365.venuesound.audio.NodeKind;
import dev.tonyd365.venuesound.audio.RouterState;
import dev.tonyd365.venuesound.audio.SoundRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A device in the cable graph. Stores the cables it sends out ({@link #getOutputs()}), publishes a
 * snapshot to the {@link SoundRouter} and syncs its state to clients so cables can be drawn.
 */
public abstract class AudioNodeBlockEntity extends BlockEntity {

    private final List<BlockPos> outputs = new ArrayList<>();

    protected AudioNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract NodeKind kind();

    public List<BlockPos> getOutputs() {
        return Collections.unmodifiableList(outputs);
    }

    public boolean hasOutput(BlockPos target) {
        return outputs.contains(target);
    }

    public boolean canAddOutput() {
        return outputs.size() < kind().maxOutputs();
    }

    public void addOutput(BlockPos target) {
        if (!outputs.contains(target) && canAddOutput()) {
            outputs.add(target.immutable());
            changed();
        }
    }

    /** Unplugs every cable this device sends out; frees the matching mixer inputs. */
    public void clearOutputs() {
        if (level != null) {
            for (BlockPos out : outputs) {
                if (level.getBlockEntity(out) instanceof MixerBlockEntity mixer) {
                    mixer.releaseInput(worldPosition);
                }
            }
        }
        outputs.clear();
        changed();
    }

    public DimPos dimPos() {
        return new DimPos(level.dimension(), worldPosition);
    }

    // ------------------------------------------------------------------ router

    public final void syncToRouter() {
        if (level instanceof ServerLevel serverLevel && !isRemoved()) {
            List<DimPos> outs = new ArrayList<>(outputs.size());
            for (BlockPos p : outputs) {
                outs.add(new DimPos(serverLevel.dimension(), p));
            }
            SoundRouter.get().put(serverLevel, snapshot(serverLevel, List.copyOf(outs)));
        }
    }

    protected RouterState.Node snapshot(ServerLevel level, List<DimPos> outs) {
        return new RouterState.Node(dimPos(), kind(), outs, 0, null, 0F, 0, 0.5D);
    }

    public final void removeFromRouter() {
        if (level instanceof ServerLevel) {
            SoundRouter.get().remove(dimPos());
        }
    }

    /** Save, republish to the router and send the new state to nearby clients. */
    protected void changed() {
        setChanged();
        syncToRouter();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        syncToRouter();
    }

    @Override
    public void setRemoved() {
        removeFromRouter();
        super.setRemoved();
    }

    // ------------------------------------------------------------------ persistence / sync

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (BlockPos p : outputs) {
            list.add(LongTag.valueOf(p.asLong()));
        }
        tag.put("Outputs", list);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        outputs.clear();
        for (Tag t : tag.getList("Outputs", Tag.TAG_LONG)) {
            outputs.add(BlockPos.of(((LongTag) t).getAsLong()));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
