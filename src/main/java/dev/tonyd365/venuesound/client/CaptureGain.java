package dev.tonyd365.venuesound.client;

import dev.tonyd365.venuesound.audio.NodeKind;
import dev.tonyd365.venuesound.block.AmplifierBlockEntity;
import dev.tonyd365.venuesound.block.AudioNodeBlockEntity;
import dev.tonyd365.venuesound.block.MixerBlockEntity;
import dev.tonyd365.venuesound.block.SpeakerBlockEntity;
import dev.tonyd365.venuesound.block.VideoCaptureBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side volume for a WATERFrAMES screen inside a capture zone: follows the cables from the
 * capture block (strip gain x fader x main, processors, amp level, speakers on) and scales by how
 * close the player is to the nearest reached speaker. Null when no cabled capture zone covers it.
 */
public final class CaptureGain {

    private CaptureGain() {
    }

    @Nullable
    public static Float gainAt(BlockPos screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return null;
        }
        Float best = null;
        for (VideoCaptureBlockEntity cap : VideoCaptureBlockEntity.CLIENT) {
            AABB zone = cap.zone();
            if (cap.getLevel() != mc.level || zone == null || !zone.inflate(0.01).contains(Vec3.atCenterOf(screen)) || cap.getLinks().isEmpty()) {
                continue;
            }
            float g = walk(mc.level, cap, 1F, false, mc.player.position(), new HashSet<>(), 0);
            best = best == null ? g : Math.max(best, g);
        }
        return best;
    }

    private static float walk(Level level, AudioNodeBlockEntity from, float g, boolean after, Vec3 player, Set<BlockPos> seen, int depth) {
        if (depth > 32 || g <= 0F) {
            return 0F;
        }
        float best = 0F;
        for (AudioNodeBlockEntity.Link l : from.getLinks()) {
            if (!seen.add(l.target()) || !(level.getBlockEntity(l.target()) instanceof AudioNodeBlockEntity next)) {
                continue;
            }
            float r = 0F;
            if (next instanceof MixerBlockEntity m) {
                if (after) {
                    continue;
                }
                int slot = -1;
                for (int i = 0; i < MixerBlockEntity.INPUTS; i++) {
                    if (from.getBlockPos().equals(m.getInput(i))) {
                        slot = i;
                    }
                }
                if (slot < 0 || m.isMuted(slot)) {
                    continue;
                }
                float strip = g * m.get("gain." + slot) * m.get("fader." + slot);
                for (AudioNodeBlockEntity.Link out : m.getLinks()) {
                    if (out.fromPort() > 1 || !(level.getBlockEntity(out.target()) instanceof AudioNodeBlockEntity t)) {
                        continue;
                    }
                    float main = m.get(out.fromPort() == 0 ? "main.l" : "main.r");
                    r = Math.max(r, step(level, t, strip * main, player, new HashSet<>(seen), depth + 1));
                }
            } else {
                r = step(level, next, g, player, seen, depth + 1, after);
            }
            best = Math.max(best, r);
        }
        return best;
    }

    private static float step(Level level, AudioNodeBlockEntity n, float g, Vec3 player, Set<BlockPos> seen, int depth) {
        return step(level, n, g, player, seen, depth, true);
    }

    private static float step(Level level, AudioNodeBlockEntity n, float g, Vec3 player, Set<BlockPos> seen, int depth, boolean after) {
        if (n instanceof SpeakerBlockEntity s) {
            if (!after || !s.isOn()) {
                return 0F;
            }
            double d = player.distanceTo(Vec3.atCenterOf(s.getBlockPos()));
            return (float) (g * Math.max(0, 1 - d / Math.max(1, s.getRange())));
        }
        if (n instanceof AmplifierBlockEntity a) {
            if (!a.isOn() || (!after && a.kind() == NodeKind.AMPLIFIER)) {
                return 0F;
            }
            g *= a.getLevelPercent() / 100F;
        }
        seen.add(n.getBlockPos());
        return walk(level, n, g, after, player, seen, depth);
    }
}
