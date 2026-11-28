package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.block.AmplifierBlockEntity;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.MixerLayout;
import dev.tonyd365.venuesound.block.ProcessorLayout;
import dev.tonyd365.venuesound.block.ScrollControl;
import dev.tonyd365.venuesound.block.SpeakerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Shows the name and value of the knob or fader under the crosshair, just below it, like the
 * readout on a digital console: "CH 3  GAIN  +6.0 dB", "MAIN L  -3.1 dB", "PAN  L 40".
 */
public final class ControlHud {

    private ControlHud() {
    }

    public static void render(GuiGraphics g, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.options.hideGui || mc.screen != null
                || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        ScrollControl.Target t = ScrollControl.find(mc.level, hit.getBlockPos(), hit.getDirection(), hit.getLocation());
        String text = t == null ? null : describe(mc, t);
        if (text == null) {
            return;
        }
        int w = mc.font.width(text);
        int x = g.guiWidth() / 2 - w / 2, y = g.guiHeight() / 2 + 12;
        g.fill(x - 4, y - 3, x + w + 4, y + 11, 0xA0000000);
        g.drawString(mc.font, text, x, y, 0xFFE8B84A, false);
    }

    @Nullable
    private static String describe(Minecraft mc, ScrollControl.Target t) {
        var be = mc.level.getBlockEntity(t.device());
        if (be instanceof MixerBlockEntity m) {
            MixerLayout.Control c = m.layout().byParam(t.id());
            if (c == null) {
                return null;
            }
            return name(m.layout(), c) + "   " + value(c, m.get(c.param()));
        }
        if (be instanceof AmplifierBlockEntity amp) {
            if (t.id().equals("amp.level")) {
                return "LEVEL   " + amp.getLevelPercent() + " %";
            }
            for (ProcessorLayout.Ctl c : ProcessorLayout.controls(amp.deviceKey())) {
                if (c.param().equals(t.id())) {
                    return c.label() + "   " + number(amp.get(c.param()), c.min(), c.max());
                }
            }
            return null;
        }
        if (be instanceof SpeakerBlockEntity s && t.id().equals("speaker.range")) {
            return "COVERAGE   " + s.getRange() + " m";
        }
        return null;
    }

    /** "CH 3  GAIN", "CH 9/10 USB  FADER", "MAIN L", "AUX 1 MASTER". */
    private static String name(MixerLayout layout, MixerLayout.Control c) {
        String p = c.param();
        int dot = p.lastIndexOf('.');
        String tail = p.substring(dot + 1);
        if (tail.chars().allMatch(Character::isDigit) && !tail.isEmpty()) {
            String strip = "CH " + layout.stripName(Integer.parseInt(tail));
            return strip + "  " + (c.kind() == MixerLayout.Kind.FADER ? "FADER" : c.label());
        }
        return switch (p) {
            case "main.l" -> "MAIN L";
            case "main.r" -> "MAIN R";
            case "aux1.m" -> "AUX 1 MASTER";
            case "aux2.m" -> "AUX 2 MASTER";
            case "efx.m" -> "EFX RETURN";
            default -> c.label();
        };
    }

    private static String value(MixerLayout.Control c, float v) {
        String p = c.param();
        if (c.kind() == MixerLayout.Kind.FADER || p.startsWith("gain.")) {
            return db(v);
        }
        if (p.startsWith("high.") || p.startsWith("mid.") || p.startsWith("low.")) {
            return String.format(Locale.ROOT, "%+.0f dB", v);
        }
        if (p.startsWith("pan.")) {
            int pct = Math.round(Math.abs(v) * 100);
            return pct < 2 ? "C" : (v < 0 ? "L " : "R ") + pct;
        }
        return Math.round((v - c.min()) / (c.max() - c.min()) * 100) + " %";
    }

    /** A gain (1 = unity) in decibels. */
    private static String db(float v) {
        if (v <= 0.001F) {
            return "-∞ dB";
        }
        return String.format(Locale.ROOT, "%+.1f dB", 20 * Math.log10(v));
    }

    private static String number(float v, double min, double max) {
        if (min < 0 && max > 0 && Math.abs(max) >= 6) {
            return String.format(Locale.ROOT, "%+.0f dB", v);
        }
        if (max <= 1.0001 && min >= 0) {
            return Math.round(v * 100) + " %";
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
