package com.martelstudios.openquests.extension.quests.block;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How many blocks each player broke back out of a player's placement and did not place again yet,
 * so a place quest guarding against abuse does not count them twice. Saved with the quest.
 */
public final class BlockRecoveries {

    /**
     * Per player id. A quest shared across worlds is reached from several threads.
     */
    final Map<String, Integer> recovered = new ConcurrentHashMap<>();

    /**
     * Notes a block the player broke out of a placement.
     */
    public void record(@Nonnull UUID playerId) {
        recovered.merge(playerId.toString(), 1, Integer::sum);
    }

    /**
     * @return whether the block placed is one the player got back that way, which is then spent.
     */
    public boolean consume(@Nonnull UUID playerId) {
        String player = playerId.toString();
        int available = recovered.getOrDefault(player, 0);
        if (available <= 0) return false;

        if (available > 1) recovered.put(player, available - 1);
        else recovered.remove(player);

        return true;
    }

    /**
     * Forgets everything, once the quest has nothing left to guard.
     */
    public void clear() {
        recovered.clear();
    }
}
