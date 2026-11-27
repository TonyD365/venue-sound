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

public class SpeakerBlockEntity extends RoutedBlockEntity implements Patchable {

    /** Coverage steps in blocks, cycled by right-clicking the speaker. */
    public static final int[] RANGES = {16, 32, 48, 64, 96, 128};

    private int range = 48;
    @Nullable
    private BlockPos mixer;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SPEAKER_BE, pos, state);
    }

    public int getRange() {
        return range;
    }

    public void cycleRange(boolean backwards) {
        int index = 0;
        for (int i = 0; i < RANGES.length; i++) {
            if (RANGES[i] == range) {
                index = i;
            }
        }
        range = RANGES[Math.floorMod(index + (backwards ? -1 : 1), RANGES.length)];
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
        SoundRouter.get().putSpeaker(level, new RouterState.Speaker(dimPos(), mixerPos, range));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Range", range);
        putPos(tag, "Mixer", mixer);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        range = tag.contains("Range") ? tag.getInt("Range") : 48;
        mixer = getPos(tag, "Mixer");
    }
}
