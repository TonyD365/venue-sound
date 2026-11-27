package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.net.DeckPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Media deck: step through the tracks in venuesound_media/, play, stop and loop. */
public class DeckScreen extends Screen {

    private static final int W = 260;
    private static final int H = 120;

    private final BlockPos pos;
    private final List<String> tracks;
    private int index;
    private boolean loop;
    private boolean playing;
    private Button loopButton;

    public DeckScreen(BlockPos pos, List<String> tracks, String current, boolean loop, boolean playing) {
        super(Component.translatable("screen.venuesound.deck"));
        this.pos = pos;
        this.tracks = tracks;
        this.index = Math.max(0, tracks.indexOf(current));
        this.loop = loop;
        this.playing = playing;
    }

    @Override
    protected void init() {
        int l = (width - W) / 2;
        int t = (height - H) / 2;
        boolean empty = tracks.isEmpty();
        addRenderableWidget(Button.builder(Component.literal("◀"), b -> step(-1)).bounds(l + 10, t + 34, 20, 20).build()).active = !empty;
        addRenderableWidget(Button.builder(Component.literal("▶"), b -> step(1)).bounds(l + W - 30, t + 34, 20, 20).build()).active = !empty;
        addRenderableWidget(Button.builder(Component.translatable("screen.venuesound.play"), b -> {
            send(DeckPackets.SELECT, currentTrack());
            send(DeckPackets.PLAY, "");
            playing = true;
        }).bounds(l + 10, t + 66, 74, 20).build()).active = !empty;
        addRenderableWidget(Button.builder(Component.translatable("screen.venuesound.stop"), b -> {
            send(DeckPackets.STOP, "");
            playing = false;
        }).bounds(l + 93, t + 66, 74, 20).build());
        loopButton = addRenderableWidget(Button.builder(loopLabel(), b -> {
            loop = !loop;
            b.setMessage(loopLabel());
            send(DeckPackets.LOOP, Boolean.toString(loop));
        }).bounds(l + 176, t + 66, 74, 20).build());
    }

    private Component loopLabel() {
        return Component.translatable(loop ? "screen.venuesound.loop_on" : "screen.venuesound.loop_off");
    }

    private String currentTrack() {
        return tracks.isEmpty() ? "" : tracks.get(index);
    }

    private void step(int dir) {
        if (tracks.isEmpty()) {
            return;
        }
        index = Math.floorMod(index + dir, tracks.size());
        send(DeckPackets.SELECT, currentTrack());
    }

    private void send(int action, String value) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeVarInt(action);
        buf.writeUtf(value, 256);
        ClientPlayNetworking.send(DeckPackets.ACTION, buf);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int l = (width - W) / 2;
        int t = (height - H) / 2;
        g.fill(l, t, l + W, t + H, 0xF0202226);
        g.fill(l, t, l + W, t + 1, 0xFF5A5F66);
        g.drawString(font, title, l + 10, t + 8, 0xFFE8B84A, false);
        g.drawString(font, playing ? Component.translatable("screen.venuesound.playing") : Component.translatable("screen.venuesound.stopped"),
                l + W - 70, t + 8, playing ? 0xFF3DDC84 : 0xFF9AA0A6, false);
        g.fill(l + 34, t + 34, l + W - 34, t + 54, 0xFF0B2A12);
        String name = tracks.isEmpty() ? "" : (index + 1) + "/" + tracks.size() + "  " + currentTrack();
        Component shown = tracks.isEmpty() ? Component.translatable("screen.venuesound.no_tracks") : Component.literal(name);
        g.drawCenteredString(font, font.substrByWidth(shown, W - 76).getString(), l + W / 2, t + 40, 0xFF3DDC84);
        g.drawCenteredString(font, Component.translatable("screen.venuesound.deck_help"), l + W / 2, t + 98, 0xFF9AA0A6);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
