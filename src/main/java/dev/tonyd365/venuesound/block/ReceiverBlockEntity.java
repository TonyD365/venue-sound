package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.DimPos;
import dev.tonyd365.venuesound.audio.RouterState;
import dev.tonyd365.venuesound.audio.SoundRouter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ReceiverBlockEntity extends RoutedBlockEntity implements Patchable {

    public static final int CHANNELS = 8;

    private int channel = 1;
    @Nullable
    private BlockPos mixer;

    public ReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.RECEIVER_BE, pos, state);
    }

    public int getChannel() {
        return channel;
    }

    public void cycleChannel(boolean backwards) {
        channel = Math.floorMod(channel - 1 + (backwards ? -1 : 1), CHANNELS) + 1;
        changed();
    }

    @Override
    public void setMixer(@Nullable BlockPos mixer) {
        this.mixer = mixer;
        changed();
    }

    @Nullable
    @Override
    public BlockPos getMixer() {
        return mixer;
    }

    @Override
    protected void publish(ServerLevel level) {
        DimPos mixerPos = mixer == null ? null : new DimPos(level.dimension(), mixer);
        SoundRouter.get().putReceiver(new RouterState.Receiver(dimPos(), channel, mixerPos));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Channel", channel);
        putPos(tag, "Mixer", mixer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        channel = Math.max(1, Math.min(CHANNELS, tag.getInt("Channel")));
        mixer = getPos(tag, "Mixer");
    }
}
