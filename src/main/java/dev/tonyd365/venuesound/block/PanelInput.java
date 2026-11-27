package dev.tonyd365.venuesound.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holding right-click repeats a block click every few ticks, which is what lets players drag
 * faders. Buttons and switches use this so one press toggles once.
 */
public final class PanelInput {

    private static final long PRESS_TICKS = 8;
    private static final Map<UUID, Press> LAST = new ConcurrentHashMap<>();

    private record Press(long pos, String control, long tick) {
    }

    private PanelInput() {
    }

    /** True for a fresh press of {@code control}; false while the same button is held. */
    public static boolean press(Player player, BlockPos pos, String control) {
        long now = player.level().getGameTime();
        Press last = LAST.get(player.getUUID());
        LAST.put(player.getUUID(), new Press(pos.asLong(), control, now));
        return last == null || last.pos() != pos.asLong() || !last.control().equals(control) || now - last.tick() > PRESS_TICKS;
    }
}
