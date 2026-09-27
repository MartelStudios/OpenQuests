package com.martelstudios.openquests.extension.quests.item;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What each player threw and picked back up while holding one quest, so a quest guarding against
 * abuse counts only what a throw and pickup loop did not bring back. Saved with the quest: leaving
 * and coming back does not wipe what was thrown.
 */
public final class ExchangeCounters {

    /**
     * Per player id, thrown and not picked back up yet. A quest shared across worlds is reached from
     * several threads.
     */
    final Map<String, Integer> thrown = new ConcurrentHashMap<>();

    /**
     * Per player id, picked back up after a throw and not thrown again yet.
     */
    final Map<String, Integer> recovered = new ConcurrentHashMap<>();

    /**
     * @return the part of the throw that had not just been picked back up after an earlier one.
     */
    public int recordThrow(@Nonnull UUID playerId, int quantity) {
        String player = playerId.toString();

        int thrownAgain = take(recovered, player, quantity);
        thrown.merge(player, quantity, Integer::sum);

        return quantity - thrownAgain;
    }

    /**
     * @return the part of the pickup the player had not thrown themselves.
     */
    public int recordGroundPickup(@Nonnull UUID playerId, int quantity) {
        String player = playerId.toString();

        int ownThrow = take(thrown, player, quantity);
        if (ownThrow > 0) recovered.merge(player, ownThrow, Integer::sum);

        return quantity - ownThrow;
    }

    /**
     * Takes up to {@code wanted} from the player's count, dropping the entry once it is spent so a
     * saved quest only lists what is still owed.
     */
    private static int take(@Nonnull Map<String, Integer> counts, @Nonnull String player, int wanted) {
        int available = counts.getOrDefault(player, 0);
        int taken = Math.min(available, wanted);

        if (available - taken > 0) counts.put(player, available - taken);
        else counts.remove(player);

        return taken;
    }
}
