package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.MediaPlayback;
import dev.tonyd365.venuesound.audio.NodeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Media deck: plays an audio file from the server's venuesound_media folder into the cables. */
public class MediaDeckBlockEntity extends AudioNodeBlockEntity {

    private String track = "";
    private boolean loop;

    public MediaDeckBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MEDIA_DECK_BE, pos, state);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.MEDIA_DECK;
    }

    public String getTrack() {
        return track;
    }

    public boolean isLoop() {
        return loop;
    }

    public void setTrack(String track) {
        this.track = track;
        changed();
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
        changed();
        MediaPlayback.get().setLoop(dimPos(), loop);
    }

    public void play() {
        if (level instanceof ServerLevel && !track.isEmpty()) {
            MediaPlayback.get().play(dimPos(), track, loop);
        }
    }

    public void stop() {
        if (level instanceof ServerLevel) {
            MediaPlayback.get().stop(dimPos());
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel) {
            MediaPlayback.get().stop(dimPos());
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Track", track);
        tag.putBoolean("Loop", loop);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        track = tag.getString("Track");
        loop = tag.getBoolean("Loop");
    }
}
