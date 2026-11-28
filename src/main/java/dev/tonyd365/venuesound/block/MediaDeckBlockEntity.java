package dev.tonyd365.venuesound.block;

import dev.tonyd365.venuesound.Registration;
import dev.tonyd365.venuesound.audio.MediaPlayback;
import dev.tonyd365.venuesound.audio.NodeKind;
import net.minecraft.core.BlockPos;
import dev.tonyd365.venuesound.compat.Nbt;
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
    public String deviceKey() {
        return "media_deck";
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

    /** A front-panel button: prev, play, stop, next or loop. */
    public void press(String button) {
        switch (button) {
            case "prev", "next" -> {
                java.util.List<String> tracks = MediaPlayback.listTracks();
                if (tracks.isEmpty()) {
                    return;
                }
                int i = Math.max(0, tracks.indexOf(track));
                if (tracks.contains(track)) {
                    i = Math.floorMod(i + ("next".equals(button) ? 1 : -1), tracks.size());
                }
                boolean wasPlaying = level instanceof ServerLevel && MediaPlayback.get().isPlaying(dimPos());
                setTrack(tracks.get(i));
                if (wasPlaying) {
                    play();
                }
            }
            case "play" -> {
                if (track.isEmpty()) {
                    java.util.List<String> tracks = MediaPlayback.listTracks();
                    if (!tracks.isEmpty()) {
                        setTrack(tracks.get(0));
                    }
                }
                play();
                setPlayingFlag(true);
            }
            case "stop" -> {
                stop();
                setPlayingFlag(false);
            }
            case "loop" -> setLoop(!loop);
            default -> {
            }
        }
    }

    /** Whether the deck shows as playing on clients (the display and the play LED). */
    private boolean playing;

    public boolean isPlayingShown() {
        return playing;
    }

    private void setPlayingFlag(boolean value) {
        playing = value;
        changed();
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
    protected void write(CompoundTag tag) {
        super.write(tag);
        tag.putString("Track", track);
        tag.putBoolean("Loop", loop);
        tag.putBoolean("Playing", playing);
    }

    @Override
    protected void read(CompoundTag tag) {
        super.read(tag);
        track = Nbt.getString(tag, "Track");
        loop = Nbt.getBoolean(tag, "Loop");
        playing = Nbt.getBoolean(tag, "Playing");
    }
}
