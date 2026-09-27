package com.martelstudios.openquests.extension.quests.block;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The blocks a quest's players placed while holding it, so that a quest guarding against abuse does
 * not count placing and breaking the same block over and over. Saved with the quest. A block carries
 * no data of its own short of a block entity, one per placed block, which every world save would pay
 * for; the quest only pays while it guards.
 */
public final class PlacedBlocks {

    /**
     * Positions as {@code world:x:y:z}, shared by the quest's players: one placing for another to
     * break is the same loop. A quest shared across worlds is reached from several threads.
     */
    final Set<String> positions = ConcurrentHashMap.newKeySet();

    /**
     * Per player id, blocks broken back out of these positions and not placed again yet.
     */
    final Map<String, Integer> recovered = new ConcurrentHashMap<>();

    /**
     * @return the key a position is remembered under, the world included.
     */
    @Nonnull
    public static String position(@Nonnull String world, int x, int y, int z) {
        return world + ":" + x + ":" + y + ":" + z;
    }

    /**
     * @return {@code false} when the block placed is one the player broke back out of a remembered
     * position, which a quest guarding against abuse leaves out.
     */
    public boolean recordPlacement(@Nonnull UUID playerId, @Nonnull String position) {
        positions.add(position);

        String player = playerId.toString();
        int available = recovered.getOrDefault(player, 0);
        if (available <= 0) return true;

        if (available > 1) recovered.put(player, available - 1);
        else recovered.remove(player);

        return false;
    }

    /**
     * @return {@code true} when the block broken was placed by one of the quest's players, which a
     * quest guarding against abuse leaves out.
     */
    public boolean recordBreak(@Nonnull UUID playerId, @Nonnull String position) {
        if (!positions.remove(position)) return false;

        recovered.merge(playerId.toString(), 1, Integer::sum);
        return true;
    }

    /**
     * Forgets everything, once the quest has nothing left to guard.
     */
    public void clear() {
        positions.clear();
        recovered.clear();
    }
}
