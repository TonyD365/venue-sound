package dev.tonyd365.venuesound.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.tonyd365.venuesound.block.DeviceBlock;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.MixerLayout;
import net.minecraft.client.renderer.MultiBufferSource;

/** Draws the console's moving parts from its current settings, plus its cables. */
public class MixerRenderer extends CableRenderer<MixerBlockEntity> {

    private static final int CAP = 0xD9DCE0;
    private static final int CAP_MASTER = 0xD0312B;
    private static final int[] KNOB_CAPS = {0xE74C3C, 0xE67E22, 0xF1C40F, 0x2ECC71, 0x1ABC9C, 0x3498DB, 0x9B59B6, 0xECF0F1};

    @Override
    public void render(MixerBlockEntity mixer, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        super.render(mixer, partialTick, pose, buffers, light, overlay);
        PanelDraw d = PanelDraw.begin(pose, buffers, mixer.getBlockState().getValue(DeviceBlock.FACING));
        double top = MixerLayout.TOP;
        for (int i = 0; i < MixerBlockEntity.INPUTS; i++) {
            double u = MixerLayout.stripU(i);
            // fader cap
            double fd = MixerLayout.faderDepth(mixer.getGain(i) / MixerBlockEntity.MAX_GAIN);
            d.box(u - 0.75, u + 0.75, fd - 0.45, fd + 0.45, top, top + 0.9, CAP, light);
            d.box(u - 0.75, u + 0.75, fd - 0.06, fd + 0.06, top + 0.9, top + 0.95, 0x222222, light);
            // mute button, lit red when muted
            boolean muted = mixer.isMuted(i);
            d.box(u - 0.6, u + 0.6, MixerLayout.MUTE_MIN + 0.1, MixerLayout.MUTE_MAX - 0.1, top, top + (muted ? 0.25 : 0.45),
                    muted ? 0xFF3030 : 0x3A3D42, muted ? PanelDraw.FULL_BRIGHT : light);
            // trim knob with a pointer: -135 deg at 0 %, +135 deg at 200 %
            double k = MixerLayout.KNOB_DEPTH;
            d.box(u - 0.8, u + 0.8, k - 0.8, k + 0.8, top, top + 0.8, 0x1A1B1E, light);
            d.box(u - 0.45, u + 0.45, k - 0.45, k + 0.45, top + 0.8, top + 0.95, KNOB_CAPS[i], light);
            double a = Math.toRadians(-135 + mixer.getTrim(i) / MixerBlockEntity.MAX_GAIN * 270);
            double pu = u + Math.sin(a) * 0.6;
            double pd = k + Math.cos(a) * 0.6;
            d.box(pu - 0.15, pu + 0.15, pd - 0.15, pd + 0.15, top + 0.8, top + 1.0, 0xFFFFFF, light);
            // signal LED: green when something is plugged into this input
            boolean patched = mixer.getInput(i) != null;
            d.box(u - 0.25, u + 0.25, 11.4, 11.8, top, top + 0.15, patched ? 0x3DDC84 : 0x2A2C30, patched ? PanelDraw.FULL_BRIGHT : light);
        }
        double mu = MixerLayout.MASTER_U;
        double md = MixerLayout.faderDepth(mixer.getMaster() / MixerBlockEntity.MAX_GAIN);
        d.box(mu - 0.85, mu + 0.85, md - 0.5, md + 0.5, top, top + 0.95, CAP_MASTER, light);
        d.box(mu - 0.85, mu + 0.85, md - 0.06, md + 0.06, top + 0.95, top + 1.0, 0x222222, light);
        // master level meter: LEDs up to the master level
        int lit = Math.round(mixer.getMaster() / MixerBlockEntity.MAX_GAIN * 8);
        for (int s = 0; s < 8; s++) {
            int col = s < 5 ? 0x3DDC84 : s < 7 ? 0xF1C40F : 0xE74C3C;
            boolean on = s < lit;
            double dd = 8.6 + s * 0.35;
            d.box(26.0, 26.6, dd, dd + 0.25, top, top + 0.1, on ? col : 0x2A2C30, on ? PanelDraw.FULL_BRIGHT : light);
        }
        d.end();
    }

    @Override
    public boolean shouldRenderOffScreen(MixerBlockEntity be) {
        return true;
    }
}
