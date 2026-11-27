package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.audio.DimPos;
import dev.tonyd365.venuesound.audio.SoundRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Base for devices that take part in the signal chain and publish their settings to the router. */
public abstract class RoutedBlockEntity extends BlockEntity {

    protected RoutedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public DimPos dimPos() {
        return new DimPos(level.dimension(), worldPosition);
    }

    /** Publish this device's current settings to the router (server side only). */
    public final void syncToRouter() {
        if (level instanceof ServerLevel serverLevel && !isRemoved()) {
            publish(serverLevel);
        }
    }

    public final void removeFromRouter() {
        if (level instanceof ServerLevel) {
            SoundRouter.get().remove(dimPos());
        }
    }

    protected abstract void publish(ServerLevel level);

    /** Save, republish to the router and send the new state to nearby clients. */
    protected void changed() {
        setChanged();
        syncToRouter();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        // Covers both freshly placed devices and ones loaded from disk.
        syncToRouter();
    }

    @Override
    public void setRemoved() {
        removeFromRouter();
        super.setRemoved();
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

    protected static void putPos(CompoundTag tag, String key, @Nullable BlockPos pos) {
        if (pos != null) {
            tag.put(key, NbtUtils.writeBlockPos(pos));
        }
    }

    @Nullable
    protected static BlockPos getPos(CompoundTag tag, String key) {
        return tag.contains(key) ? NbtUtils.readBlockPos(tag.getCompound(key)) : null;
    }
}
