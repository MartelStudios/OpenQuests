package com.martelstudios.openquests.core.assignments;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The period each holder was last found settled for, by holder key and then by assignment and
 * quest, so a period reached again and again costs no read once it is handed out. Only ever true
 * for what this server saw handed out: another server handing a period out first is found on the
 * next read instead.
 */
final class SettledOccasions {

    private final Map<String, Map<String, String>> byHolder = new ConcurrentHashMap<>();

    /**
     * @return whether every quest the assignment lists was already settled with that holder for
     * that occasion.
     */
    boolean isSettled(@Nonnull String holderKey, @Nonnull OpenQuestAssignment assignment, @Nonnull String occasion) {
        Map<String, String> byQuest = byHolder.get(holderKey);
        if (byQuest == null) return false;

        for (String questAssetId : assignment.getQuestAssetIds()) {
            if (!occasion.equals(byQuest.get(assignment.getId() + "/" + questAssetId))) return false;
        }
        return true;
    }

    void settle(@Nonnull String holderKey, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nonnull String occasion) {
        byHolder.computeIfAbsent(holderKey, key -> new ConcurrentHashMap<>()).put(assignmentId + "/" + questAssetId, occasion);
    }

    /**
     * Drops a holder gone for now or for good: a player leaving, a world closing.
     */
    void forget(@Nonnull String holderKey) {
        byHolder.remove(holderKey);
    }
}
