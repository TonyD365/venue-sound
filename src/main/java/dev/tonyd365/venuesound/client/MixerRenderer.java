package dev.tonyd365.venuesound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.MixerLayout;
import net.minecraft.client.renderer.MultiBufferSource;

/** Draws every control of the console from {@link MixerLayout} and the console's current settings. */
public class MixerRenderer extends CableRenderer<MixerBlockEntity> {

    @Override
    public void render(MixerBlockEntity mixer, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        super.render(mixer, partialTick, pose, buffers, light, overlay);
    }

    @Override
    protected void renderPanel(MixerBlockEntity m, PanelDraw d, MultiBufferSource buffers, int light) {
        double top = MixerLayout.TOP;
        MixerLayout layout = m.layout();
        boolean active = m.isUsbPlaying();
        for (int i = 0; i < MixerBlockEntity.INPUTS; i++) {
            active |= m.getInput(i) != null;
        }
        // raised rear jack field
        d.box(0.4, layout.width - 12.4, 28.4, 31.9, top, top + 1.2, 0x1C1D20, light);
        for (MixerLayout.Control c : layout.controls()) {
            double u = c.u(), dd = c.depth();
            switch (c.kind()) {
                case KNOB -> {
                    float v = m.get(c.param());
                    double r = MixerLayout.KNOB_R;
                    d.box(u - r, u + r, dd - r, dd + r, top, top + 0.45, 0x141518, light);
                    d.box(u - r * 0.68, u + r * 0.68, dd - r * 0.68, dd + r * 0.68, top + 0.45, top + 1.0, c.color(), light);
                    double a = MixerLayout.knobAngle(c, v);
                    double pu = u + Math.sin(a) * r * 0.55, pd = dd + Math.cos(a) * r * 0.55;
                    d.box(pu - 0.12, pu + 0.12, pd - 0.12, pd + 0.12, top + 1.0, top + 1.08, 0xFFFFFF, light);
                    if (u > layout.width - 12) {
                        d.topText(buffers, c.label(), u - 1.0, dd - 1.0, top, 0.45, 0xFFB8BEC4); // master: under the knob
                    } else {
                        d.topText(buffers, c.label(), u + 0.9, dd + 0.18, top, 0.34, 0xFFB8BEC4); // strips: right of the knob
                    }
                }
                case FADER -> {
                    float v = m.get(c.param());
                    d.box(u - 0.14, u + 0.14, MixerLayout.FADER_MIN - 0.4, MixerLayout.FADER_MAX + 0.4, top, top + 0.02, 0x050506, light);
                    for (int k = 0; k <= 8; k++) {
                        double tick = MixerLayout.faderDepth(k / 8.0);
                        d.box(u - 0.75, u - 0.42, tick - 0.04, tick + 0.04, top, top + 0.02, k == 4 ? 0xE8B84A : 0x6B7178, light);
                    }
                    double fd = MixerLayout.faderDepth(v / 2.0);
                    d.box(u - 0.62, u + 0.62, fd - 0.5, fd + 0.5, top, top + 0.55, 0x1A1B1E, light);
                    d.box(u - 0.6, u + 0.6, fd - 0.42, fd + 0.42, top + 0.55, top + 1.15, c.color(), light);
                    d.box(u - 0.6, u + 0.6, fd - 0.05, fd + 0.05, top + 1.15, top + 1.18, 0x111111, light);
                    d.topText(buffers, c.label(), u - 0.5, 0.95, top, 0.5, 0xFFE8E8E8);
                }
                case BUTTON -> {
                    boolean on = c.param().startsWith("usb.")
                            ? c.param().equals("usb.play") && m.isUsbPlaying()
                            : m.get(c.param()) > 0.5F;
                    double hw = c.w() / 2, hd = c.d() / 2;
                    d.box(u - hw, u + hw, dd - hd, dd + hd, top, top + (on ? 0.3 : 0.5),
                            on ? c.color() : 0x3A3D42, on ? PanelDraw.FULL_BRIGHT : light);
                    d.topText(buffers, c.label(), u - hw + 0.15, dd + 0.25, top + (on ? 0.3 : 0.5), 0.42, on ? 0xFF111111 : 0xFFDDDDDD);
                }
                case LED -> {
                    boolean on;
                    if (c.param().startsWith("peak.")) {
                        int s = Integer.parseInt(c.param().substring(5));
                        on = m.get("gain." + s) * m.get("fader." + s) > 1.6F && !m.isMuted(s);
                    } else {
                        int step = Integer.parseInt(c.param().substring(c.param().lastIndexOf('.') + 1));
                        float lvl = c.param().startsWith("meter.l") ? m.get("main.l") : m.get("main.r");
                        on = active && step < Math.round(lvl / 2F * 8);
                    }
                    d.box(u - 0.22, u + 0.22, dd - 0.22, dd + 0.22, top, top + 0.12, on ? c.color() : 0x2A2C30, on ? PanelDraw.FULL_BRIGHT : light);
                }
                case JACK -> {
                    double r = c.w() / 2, h = top + 1.2;
                    d.box(u - r, u + r, dd - r, dd + r, h, h + 0.15, 0x9AA0A6, light);
                    d.box(u - r * 0.6, u + r * 0.6, dd - r * 0.6, dd + r * 0.6, h + 0.15, h + 0.18, 0x050506, light);
                }
                case DISPLAY -> {
                    double hw = c.w() / 2, hd = c.d() / 2;
                    d.box(u - hw - 0.3, u + hw + 0.3, dd - hd - 0.3, dd + hd + 0.3, top, top + 0.1, 0x111111, light);
                    d.box(u - hw, u + hw, dd - hd, dd + hd, top, top + 0.15, 0x0B2A12, PanelDraw.FULL_BRIGHT);
                    String track = m.getUsbTrack().isEmpty() ? "NO USB TRACK" : m.getUsbTrack();
                    if (track.length() > 22) {
                        track = track.substring(0, 21) + "~";
                    }
                    d.topText(buffers, "USB PLAYER  " + (m.isUsbPlaying() ? "> PLAY" : "[] STOP"), u - hw + 0.4, dd + hd - 0.6, top + 0.15, 0.7, 0xFF3DDC84);
                    d.topText(buffers, track, u - hw + 0.4, dd + 0.2, top + 0.15, 0.7, 0xFF3DDC84);
                }
            }
        }
        // strip numbers along the front edge
        for (int i = 0; i < layout.strips; i++) {
            d.topText(buffers, layout.stripName(i), MixerLayout.stripU(i) - 1.4, 28.9, MixerLayout.TOP + 1.2, 0.5, 0xFFE8B84A);
        }
        d.topText(buffers, "VENUESOUND  " + layout.model, layout.width - 10.4, 31.2, MixerLayout.TOP, 0.75, 0xFFE8B84A);
    }

    @Override
    public boolean shouldRenderOffScreen(MixerBlockEntity be) {
        return true;
    }
}
