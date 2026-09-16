package com.martelstudios.openquests.extension.quests.queststate;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which quests read an asset's outcome, and which quests a player holds from it. A cache of the
 * quest store rather than an authority: everything here is put back by the store's own events.
 */
public final class QuestStateIndex {

    /**
     * Watched asset id, then player, then the quest-state quests reading it.
     */
    private static final Map<String, Map<UUID, Set<UUID>>> watchers = new ConcurrentHashMap<>();

    /**
     * Asset id, then player, then the quests they hold from it. Several runs of one asset can be
     * held at once and any of them may satisfy, so an answer is read off the whole set.
     */
    private static final Map<String, Map<UUID, Set<UUID>>> candidates = new ConcurrentHashMap<>();

    private QuestStateIndex() {}

    public static void track(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        add(candidates, quest.getAssetId(), playerId, quest.getId());
        add(watchers, watchedAssetOf(quest), playerId, quest.getId());
    }

    public static void forget(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        remove(candidates, quest.getAssetId(), playerId, quest.getId());
        remove(watchers, watchedAssetOf(quest), playerId, quest.getId());
    }

    /**
     * Moves a quest that changed what it watches, for everyone holding it.
     *
     * @param previousAssetId what it watched until now, without which the old entry could only be
     * found by walking the whole index.
     */
    public static void rewatch(@Nonnull QuestStateQuestProgression quest, @Nullable String previousAssetId) {
        String assetId = watchedAssetOf(quest);

        for (UUID playerId : quest.getPlayers()) {
            remove(watchers, previousAssetId, playerId, quest.getId());
            add(watchers, assetId, playerId, quest.getId());
        }

        for (UUID playerId : quest.getAbandonedPlayers()) {
            remove(watchers, previousAssetId, playerId, quest.getId());
            add(watchers, assetId, playerId, quest.getId());
        }
    }

    /**
     * @return the quest-state quests this player holds that read the asset's outcome.
     */
    @Nonnull
    public static Set<UUID> watchersOf(@Nullable String assetId, @Nonnull UUID playerId) {
        return get(watchers, assetId, playerId);
    }

    /**
     * @return what could answer for the asset: the quests this player holds that were built from it.
     */
    @Nonnull
    public static Set<UUID> candidatesOf(@Nullable String assetId, @Nonnull UUID playerId) {
        return get(candidates, assetId, playerId);
    }

    /**
     * @return what the quest watches, or {@code null} for one that watches nothing and for one
     * whose asset is gone, which is the only thing that could still name it.
     */
    @Nullable
    private static String watchedAssetOf(@Nonnull AbstractQuestProgression<?> quest) {
        if (!(quest instanceof QuestStateQuestProgression stateQuest) || stateQuest.getAsset() == null) return null;

        return stateQuest.getQuestAssetId();
    }

    /**
     * Both halves run under the outer key, so neither can walk in on the other's nesting: an add
     * landing in a map that a removal is pruning would be lost.
     */
    private static void add(@Nonnull Map<String, Map<UUID, Set<UUID>>> index, @Nullable String assetId, @Nonnull UUID playerId, @Nonnull UUID questId) {
        if (assetId == null) return;

        index.compute(assetId, (id, byPlayer) -> {
            Map<UUID, Set<UUID>> players = byPlayer != null ? byPlayer : new ConcurrentHashMap<>();
            players.computeIfAbsent(playerId, id2 -> ConcurrentHashMap.newKeySet()).add(questId);

            return players;
        });
    }

    /**
     * Drops whatever is left empty. An index that only ever grew would outlive every quest it was
     * built from, one entry per asset and per player who once held one.
     */
    private static void remove(@Nonnull Map<String, Map<UUID, Set<UUID>>> index, @Nullable String assetId, @Nonnull UUID playerId, @Nonnull UUID questId) {
        if (assetId == null) return;

        index.computeIfPresent(assetId, (id, byPlayer) -> {
            byPlayer.computeIfPresent(playerId, (id2, questIds) -> {
                questIds.remove(questId);

                return questIds.isEmpty() ? null : questIds;
            });

            return byPlayer.isEmpty() ? null : byPlayer;
        });
    }

    @Nonnull
    private static Set<UUID> get(@Nonnull Map<String, Map<UUID, Set<UUID>>> index, @Nullable String assetId, @Nonnull UUID playerId) {
        if (assetId == null) return Set.of();

        return index.getOrDefault(assetId, Map.of()).getOrDefault(playerId, Set.of());
    }
}
