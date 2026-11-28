package dev.tonyd365.venuesound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.tonyd365.venuesound.block.AmplifierBlock;
import dev.tonyd365.venuesound.block.AmplifierBlockEntity;
import dev.tonyd365.venuesound.block.DeviceBlock;
import dev.tonyd365.venuesound.block.MediaDeckBlock;
import dev.tonyd365.venuesound.block.MediaDeckBlockEntity;
import dev.tonyd365.venuesound.block.ReceiverBlock;
import dev.tonyd365.venuesound.block.ReceiverBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;

/** Renderers that draw the live front panels of the amplifier, media deck and receiver. */
public final class DeviceRenderers {

    private DeviceRenderers() {
    }

    public static class Amplifier extends CableRenderer<AmplifierBlockEntity> {
        @Override
        public void render(AmplifierBlockEntity amp, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            super.render(amp, pt, pose, buffers, light, overlay);
            PanelDraw d = PanelDraw.begin(pose, buffers, amp.getBlockState().getValue(DeviceBlock.FACING));
            boolean on = amp.isOn();
            // rocker switch: the lit half shows which way it is pressed
            double u0 = AmplifierBlock.SWITCH_U0, u1 = AmplifierBlock.SWITCH_U1, h0 = AmplifierBlock.SWITCH_H0, h1 = AmplifierBlock.SWITCH_H1;
            double mid = (h0 + h1) / 2;
            d.frontBox(u0, u1, h0, h1, 0.3, 0x15161A, light);
            d.frontBox(u0 + 0.3, u1 - 0.3, on ? mid : h0 + 0.3, on ? h1 - 0.3 : mid, on ? 0.7 : 0.5,
                    on ? 0xE74C3C : 0x3A3D42, on ? PanelDraw.FULL_BRIGHT : light);
            d.frontText(buffers, on ? "ON" : "OFF", u0, h0 - 0.6, 0, 1.2, 0xFFC9CDD2);
            // level knob with a pointer, and the level printed under it
            double ku = AmplifierBlock.KNOB_U, kh = AmplifierBlock.KNOB_H, r = AmplifierBlock.KNOB_R;
            d.frontBox(ku - r, ku + r, kh - r, kh + r, 0.8, 0x1A1B1E, light);
            d.frontBox(ku - r + 0.6, ku + r - 0.6, kh - r + 0.6, kh + r - 0.6, 1.1, 0x9AA0A6, light);
            double a = Math.toRadians(-135 + (amp.getLevelPercent() - 25) / 175.0 * 270);
            double pu = ku + Math.sin(a) * (r - 0.9);
            double ph = kh + Math.cos(a) * (r - 0.9);
            d.frontBox(pu - 0.3, pu + 0.3, ph - 0.3, ph + 0.3, 1.3, 0xFFFFFF, light);
            d.frontText(buffers, amp.getLevelPercent() + "%", ku - 1.6, kh - r - 0.5, 0, 1.2, 0xFFE8B84A);
            // processor controls (graphic EQ sliders, compressor / FX / crossover knobs)
            for (var pc : dev.tonyd365.venuesound.block.ProcessorLayout.controls(amp.deviceKey())) {
                double t = pc.t(amp.get(pc.param()));
                if (pc.slider()) {
                    double s0 = pc.SLIDER_H0, s1 = pc.SLIDER_H1, hh = s0 + t * (s1 - s0);
                    d.frontBox(pc.u() - 0.15, pc.u() + 0.15, s0, s1, 0.1, 0x050506, light);
                    d.frontBox(pc.u() - 0.6, pc.u() + 0.6, hh - 0.3, hh + 0.3, 0.6, 0xE8E8E8, light);
                    d.frontText(buffers, pc.label(), pc.u() - 0.7, s0 - 0.5, 0, 0.5, 0xFFC9CDD2);
                } else {
                    double kr = pc.KNOB_R, ka = Math.toRadians(-135 + t * 270);
                    d.frontBox(pc.u() - kr, pc.u() + kr, pc.h() - kr, pc.h() + kr, 0.7, 0x1A1B1E, light);
                    double qu = pc.u() + Math.sin(ka) * kr * 0.6, qh = pc.h() + Math.cos(ka) * kr * 0.6;
                    d.frontBox(qu - 0.2, qu + 0.2, qh - 0.2, qh + 0.2, 0.9, 0xFFFFFF, light);
                    String lbl = pc.param().equals("xo.mode") ? new String[] {"FULL", "LOW", "HIGH"}[Math.round(amp.get(pc.param()))] : pc.label();
                    d.frontText(buffers, lbl, pc.u() - kr, pc.h() - kr - 0.5, 0, 0.6, 0xFFC9CDD2);
                }
            }
            // power LED
            d.frontBox(12.6, 13.4, 13.0, 13.8, 0.2, on ? 0x3DDC84 : 0x2A2C30, on ? PanelDraw.FULL_BRIGHT : light);
            d.end();
        }
    }

    public static class Deck extends CableRenderer<MediaDeckBlockEntity> {
        private static final String[] LABELS = {"|<", ">", "[]", ">|", "LP"};

        @Override
        public void render(MediaDeckBlockEntity deck, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            super.render(deck, pt, pose, buffers, light, overlay);
            PanelDraw d = PanelDraw.begin(pose, buffers, deck.getBlockState().getValue(DeviceBlock.FACING));
            // display
            d.frontBox(2.0, 14.0, 4.2, 7.2, 0.1, 0x0B2A12, PanelDraw.FULL_BRIGHT);
            String name = deck.getTrack().isEmpty() ? "NO TRACK" : deck.getTrack();
            if (name.length() > 16) {
                name = name.substring(0, 15) + "~";
            }
            d.frontText(buffers, (deck.isPlayingShown() ? "> " : "  ") + name, 2.3, 6.8, -0.12, 1.1, 0xFF3DDC84);
            d.frontText(buffers, deck.isLoop() ? "LOOP" : "", 2.3, 5.4, -0.12, 0.9, 0xFF3DDC84);
            // buttons
            for (int i = 0; i < MediaDeckBlock.BUTTONS.length; i++) {
                double u = MediaDeckBlock.buttonU0(i);
                boolean lit = (i == 1 && deck.isPlayingShown()) || (i == 4 && deck.isLoop());
                d.frontBox(u, u + 2.0, MediaDeckBlock.BUTTON_H0, MediaDeckBlock.BUTTON_H1, 0.4,
                        lit ? 0x2ECC71 : 0x3A3D42, lit ? PanelDraw.FULL_BRIGHT : light);
                d.frontText(buffers, LABELS[i], u + 0.35, MediaDeckBlock.BUTTON_H1 - 0.5, -0.42, 1.0, 0xFFE0E0E0);
            }
            d.end();
        }
    }

    public static class Receiver extends CableRenderer<ReceiverBlockEntity> {
        @Override
        public void render(ReceiverBlockEntity rx, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            super.render(rx, pt, pose, buffers, light, overlay);
            PanelDraw d = PanelDraw.begin(pose, buffers, rx.getBlockState().getValue(DeviceBlock.FACING));
            double f = ReceiverBlock.FRONT_DEPTH;
            d.box(2.5, 8.5, f - 0.1, f + 0.01, 1.3, 4.0, 0x0B2A12, PanelDraw.FULL_BRIGHT);
            d.frontText(buffers, "CH " + rx.getChannel(), 2.9, 3.6, f - 0.12, 2.0, 0xFF3DDC84);
            for (double u0 : new double[] {ReceiverBlock.DOWN_U0, ReceiverBlock.UP_U0}) {
                d.box(u0, u0 + ReceiverBlock.BUTTON_W, f - 0.4, f + 0.01, ReceiverBlock.BUTTON_H0, ReceiverBlock.BUTTON_H1, 0x9AA0A6, light);
            }
            d.frontText(buffers, "-", ReceiverBlock.DOWN_U0 + 0.55, ReceiverBlock.BUTTON_H1 - 0.3, f - 0.42, 1.4, 0xFF111111);
            d.frontText(buffers, "+", ReceiverBlock.UP_U0 + 0.5, ReceiverBlock.BUTTON_H1 - 0.3, f - 0.42, 1.4, 0xFF111111);
            d.end();
        }
    }

    public static class Speaker extends CableRenderer<dev.tonyd365.venuesound.block.SpeakerBlockEntity> {
        @Override
        protected void renderPanel(dev.tonyd365.venuesound.block.SpeakerBlockEntity sp, PanelDraw d, MultiBufferSource buffers, int light) {
            dev.tonyd365.venuesound.block.PortLayout.SpeakerPanel panel = dev.tonyd365.venuesound.block.PortLayout.SpeakerPanel.of(sp.deviceKey());
            double depth = panel.depth();
            boolean on = sp.isOn();
            double u0 = panel.switchU0(), u1 = panel.switchU1();
            double h0 = panel.switchH0(), h1 = panel.switchH1();
            double mid = (h0 + h1) / 2;
            d.box(u0, u1, depth, depth + 0.3, h0, h1, 0x15161A, light);
            d.box(u0 + 0.25, u1 - 0.25, depth, depth + 0.55, on ? mid : h0 + 0.25, on ? h1 - 0.25 : mid,
                    on ? 0xE74C3C : 0x3A3D42, on ? PanelDraw.FULL_BRIGHT : light);
            d.backText(buffers, on ? "ON" : "OFF", u1, h0 - 0.4, depth, 0.9, 0xFFC9CDD2);
            double ku = panel.knobU();
            double kh = panel.knobH();
            double r = panel.knobR();
            d.box(ku - r, ku + r, depth, depth + 0.5, kh - r, kh + r, 0x1A1B1E, light);
            d.box(ku - r + 0.4, ku + r - 0.4, depth, depth + 0.7, kh - r + 0.4, kh + r - 0.4, 0x9AA0A6, light);
            int idx = 0;
            for (int i = 0; i < dev.tonyd365.venuesound.block.SpeakerBlockEntity.RANGES.length; i++) {
                if (dev.tonyd365.venuesound.block.SpeakerBlockEntity.RANGES[i] == sp.getRange()) {
                    idx = i;
                }
            }
            double a = Math.toRadians(-135 + idx / 5.0 * 270);
            // seen from behind, the knob turns the other way round
            double pu = ku - Math.sin(a) * (r - 0.6);
            double ph = kh + Math.cos(a) * (r - 0.6);
            d.box(pu - 0.22, pu + 0.22, depth, depth + 0.85, ph - 0.22, ph + 0.22, 0xFFFFFF, light);
            d.backText(buffers, sp.getRange() + "m", ku + 1.2, kh - r - 0.3, depth, 0.9, 0xFFE8B84A);
            d.box(panel.ledU() - 0.4, panel.ledU() + 0.4, depth, depth + 0.2, panel.ledH() - 0.4, panel.ledH() + 0.4, on ? 0x3DDC84 : 0x2A2C30, on ? PanelDraw.FULL_BRIGHT : light);
        }
    }
}
