package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.audio.DimPos;
import dev.tonyd365.venuesound.audio.NodeKind;
import dev.tonyd365.venuesound.audio.RouterState;
import dev.tonyd365.venuesound.audio.SoundRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import dev.tonyd365.venuesound.compat.Nbt;
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

    /** A cable this device sends out: to which device, from which of our sockets, into which of theirs. */
    public record Link(BlockPos target, int fromPort, int toPort, List<BlockPos> via, CableType cable) {
        public Link(BlockPos target, int fromPort, int toPort) {
            this(target, fromPort, toPort, List.of(), CableType.AUDIO);
        }
    }

    private final List<Link> links = new ArrayList<>();

    protected AudioNodeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract NodeKind kind();

    /** Key for {@link PortLayout}: the block id without the namespace. */
    public abstract String deviceKey();

    public List<Link> getLinks() {
        return Collections.unmodifiableList(links);
    }

    public List<BlockPos> getOutputs() {
        List<BlockPos> out = new ArrayList<>(links.size());
        for (Link l : links) {
            out.add(l.target());
        }
        return out;
    }

    public boolean hasOutput(BlockPos target) {
        for (Link l : links) {
            if (l.target().equals(target)) {
                return true;
            }
        }
        return false;
    }

    /** Whether a cable already runs from us into socket {@code toPort} of {@code target}. */
    public boolean hasLink(BlockPos target, int toPort) {
        for (Link l : links) {
            if (l.target().equals(target) && l.toPort() == toPort) {
                return true;
            }
        }
        return false;
    }

    /** Whether one of our sockets already has a cable going out. */
    public boolean portInUse(int fromPort) {
        for (Link l : links) {
            if (l.fromPort() == fromPort) {
                return true;
            }
        }
        return false;
    }

    public boolean canAddOutput() {
        return links.size() < kind().maxOutputs();
    }

    public void addLink(BlockPos target, int fromPort, int toPort) {
        addLink(target, fromPort, toPort, List.of());
    }

    /** Adds a cable that runs through the cable clips at {@code via} on its way. */
    public void addLink(BlockPos target, int fromPort, int toPort, List<BlockPos> via) {
        addLink(target, fromPort, toPort, via, CableType.AUDIO);
    }

    /** Adds a cable of the given type. */
    public void addLink(BlockPos target, int fromPort, int toPort, List<BlockPos> via, CableType cable) {
        if (!hasLink(target, toPort) && !portInUse(fromPort) && canAddOutput()) {
            links.add(new Link(target.immutable(), fromPort, toPort, List.copyOf(via), cable));
            changed();
        }
    }

    /** Unplugs every cable this device sends out; frees the matching mixer inputs. */
    public void clearOutputs() {
        if (level != null) {
            for (Link l : links) {
                if (level.getBlockEntity(l.target()) instanceof MixerBlockEntity mixer) {
                    mixer.releaseInput(worldPosition);
                }
            }
        }
        links.clear();
        changed();
    }

    /** Unplugs the cable leaving from one socket. */
    public void unplugPort(int fromPort) {
        Link found = null;
        for (Link l : links) {
            if (l.fromPort() == fromPort) {
                found = l;
            }
        }
        if (found != null) {
            if (level != null && level.getBlockEntity(found.target()) instanceof MixerBlockEntity mixer) {
                mixer.releaseInput(worldPosition);
            }
            links.remove(found);
            changed();
        }
    }

    public DimPos dimPos() {
        return new DimPos(level.dimension(), worldPosition);
    }

    // ------------------------------------------------------------------ router

    public final void syncToRouter() {
        if (level instanceof ServerLevel serverLevel && !isRemoved()) {
            List<RouterState.Out> outs = new ArrayList<>(links.size());
            for (Link l : links) {
                outs.add(new RouterState.Out(new DimPos(serverLevel.dimension(), l.target()), l.fromPort(), l.toPort()));
            }
            SoundRouter.get().put(serverLevel, snapshot(serverLevel, List.copyOf(outs)));
        }
    }

    protected RouterState.Node snapshot(ServerLevel level, List<RouterState.Out> outs) {
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

    /** Writes this device's state; subclasses add theirs after calling super. */
    protected void write(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Link l : links) {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", l.target().asLong());
            c.putInt("From", l.fromPort());
            c.putInt("To", l.toPort());
            if (!l.via().isEmpty()) {
                c.putLongArray("Via", l.via().stream().mapToLong(BlockPos::asLong).toArray());
            }
            if (l.cable() != CableType.AUDIO) {
                c.putString("Cable", l.cable().id);
            }
            list.add(c);
        }
        tag.put("Links", list);
    }

    /** Reads what {@link #write} wrote; subclasses read theirs after calling super. */
    protected void read(CompoundTag tag) {
        links.clear();
        for (CompoundTag c : Nbt.compounds(tag, "Links")) {
            List<BlockPos> via = new ArrayList<>();
            for (long v : Nbt.getLongArray(c, "Via")) {
                via.add(BlockPos.of(v));
            }
            links.add(new Link(BlockPos.of(Nbt.getLong(c, "Pos")), Nbt.getInt(c, "From"), Nbt.getInt(c, "To"), List.copyOf(via),
                    CableType.byId(Nbt.getString(c, "Cable"))));
        }
        // worlds from 0.2.0 stored plain positions
        for (long pos : Nbt.longs(tag, "Outputs")) {
            links.add(new Link(BlockPos.of(pos), 0, 0));
        }
    }

    //? if >=1.21 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        write(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        read(tag);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    *///?} else {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        write(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        read(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }
    //?}

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
