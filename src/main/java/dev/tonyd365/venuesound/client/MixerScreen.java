package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.net.MixerPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

/** Console-style mixer: 8 channel strips (fader + mute) and a master fader. */
public class MixerScreen extends Screen {

    private static final int STRIP_W = 34;
    private static final int FADER_H = 110;
    private static final int PANEL_W = STRIP_W * (MixerBlockEntity.INPUTS + 1) + 30;
    private static final int PANEL_H = 196;

    private final BlockPos pos;
    private final float[] gains;
    private final boolean[] mutes;
    private float master;
    private final int patchedMask;
    private final int speakers;
    private final Button[] muteButtons = new Button[MixerBlockEntity.INPUTS];

    public MixerScreen(BlockPos pos, float[] gains, boolean[] mutes, float master, int patchedMask, int speakers) {
        super(Component.translatable("screen.venuesound.mixer"));
        this.pos = pos;
        this.gains = gains;
        this.mutes = mutes;
        this.master = master;
        this.patchedMask = patchedMask;
        this.speakers = speakers;
    }

    private int left() {
        return (width - PANEL_W) / 2;
    }

    private int top() {
        return (height - PANEL_H) / 2;
    }

    @Override
    protected void init() {
        int faderTop = top() + 36;
        for (int i = 0; i < MixerBlockEntity.INPUTS; i++) {
            final int input = i;
            int x = left() + 10 + i * STRIP_W;
            addRenderableWidget(new VerticalFader(x + 9, faderTop, 14, FADER_H, gains[i] / MixerBlockEntity.MAX_GAIN, v -> {
                gains[input] = v * MixerBlockEntity.MAX_GAIN;
                send(input, gains[input], mutes[input]);
            }));
            muteButtons[i] = addRenderableWidget(Button.builder(muteLabel(i), b -> {
                mutes[input] = !mutes[input];
                b.setMessage(muteLabel(input));
                send(input, gains[input], mutes[input]);
            }).bounds(x + 4, faderTop + FADER_H + 18, 24, 16).build());
        }
        int mx = left() + 20 + MixerBlockEntity.INPUTS * STRIP_W;
        addRenderableWidget(new VerticalFader(mx + 9, faderTop, 14, FADER_H, master / MixerBlockEntity.MAX_GAIN, v -> {
            master = v * MixerBlockEntity.MAX_GAIN;
            send(MixerBlockEntity.INPUTS, master, false);
        }));
    }

    private Component muteLabel(int input) {
        return Component.literal("M").withStyle(mutes[input] ? ChatFormatting.RED : ChatFormatting.GRAY);
    }

    private void send(int input, float gain, boolean mute) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(pos);
        buf.writeVarInt(input);
        buf.writeFloat(gain);
        buf.writeBoolean(mute);
        ClientPlayNetworking.send(MixerPackets.UPDATE, buf);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        int l = left();
        int t = top();
        g.fill(l, t, l + PANEL_W, t + PANEL_H, 0xF0202226);
        g.fill(l, t, l + PANEL_W, t + 1, 0xFF5A5F66);
        g.drawString(font, title, l + 10, t + 8, 0xFFE8B84A, false);
        g.drawString(font, Component.translatable("screen.venuesound.speakers", speakers), l + 10, t + 20, 0xFF9AA0A6, false);

        int faderTop = t + 36;
        for (int i = 0; i < MixerBlockEntity.INPUTS; i++) {
            int x = l + 10 + i * STRIP_W;
            g.fill(x, t + 32, x + STRIP_W - 2, t + PANEL_H - 6, 0xFF2B2E33);
            boolean patched = (patchedMask & (1 << i)) != 0;
            g.fill(x + 3, t + 34, x + 7, t + 38, patched ? 0xFF3DDC84 : 0xFF555555);
            g.drawCenteredString(font, "CH" + (i + 1), x + 18, faderTop + FADER_H + 6, 0xFFFFFFFF);
            g.drawCenteredString(font, percent(gains[i]), x + 16, t + PANEL_H - 16, mutes[i] ? 0xFFE05050 : 0xFFCCCCCC);
        }
        int mx = l + 20 + MixerBlockEntity.INPUTS * STRIP_W;
        g.fill(mx, t + 32, mx + STRIP_W - 2, t + PANEL_H - 6, 0xFF3A2F1E);
        g.drawCenteredString(font, Component.translatable("screen.venuesound.master"), mx + 16, faderTop + FADER_H + 6, 0xFFE8B84A);
        g.drawCenteredString(font, percent(master), mx + 16, t + PANEL_H - 16, 0xFFE8B84A);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private static String percent(float gain) {
        return Math.round(gain * 100) + "%";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A vertical fader; value 0..1 from bottom to top. Reports changes while dragging. */
    private static class VerticalFader extends AbstractWidget {

        interface Listener {
            void changed(float value);
        }

        private float value;
        private final Listener listener;

        VerticalFader(int x, int y, int w, int h, float value, Listener listener) {
            super(x, y, w, h, Component.empty());
            this.value = Math.max(0F, Math.min(1F, value));
            this.listener = listener;
        }

        private void setFromMouse(double mouseY) {
            float v = 1F - (float) ((mouseY - getY()) / (double) height);
            v = Math.max(0F, Math.min(1F, v));
            // Snap to unity gain (100%) near the middle, like the 0 dB mark on a real console.
            if (Math.abs(v - 0.5F) < 0.02F) {
                v = 0.5F;
            }
            if (v != value) {
                value = v;
                listener.changed(value);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            setFromMouse(mouseY);
        }

        @Override
        protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
            setFromMouse(mouseY);
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int cx = getX() + width / 2;
            g.fill(cx - 1, getY(), cx + 1, getY() + height, 0xFF0E0F11);
            int unity = getY() + height / 2;
            g.fill(getX(), unity, getX() + width, unity + 1, 0xFF6B7178);
            int knobY = getY() + Math.round((1F - value) * height);
            g.fill(getX() - 2, knobY - 4, getX() + width + 2, knobY + 4, isHoveredOrFocused() ? 0xFFF0F0F0 : 0xFFC9CDD2);
            g.fill(getX() - 2, knobY, getX() + width + 2, knobY + 1, 0xFF222222);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
