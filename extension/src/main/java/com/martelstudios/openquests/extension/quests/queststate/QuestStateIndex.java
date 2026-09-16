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
     * Moves a quest that changed what it watches, for every player holding it.
     */
    public static void rewatch(@Nonnull QuestStateQuestProgression quest) {
        for (Map<UUID, Set<UUID>> byPlayer : watchers.values()) {
            byPlayer.values().forEach(questIds -> questIds.remove(quest.getId()));
        }

        for (UUID playerId : quest.getPlayers()) {
            add(watchers, watchedAssetOf(quest), playerId, quest.getId());
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

    private static void add(@Nonnull Map<String, Map<UUID, Set<UUID>>> index, @Nullable String assetId, @Nonnull UUID playerId, @Nonnull UUID questId) {
        if (assetId == null) return;

        index.computeIfAbsent(assetId, id -> new ConcurrentHashMap<>())
             .computeIfAbsent(playerId, id -> ConcurrentHashMap.newKeySet())
             .add(questId);
    }

    private static void remove(@Nonnull Map<String, Map<UUID, Set<UUID>>> index, @Nullable String assetId, @Nonnull UUID playerId, @Nonnull UUID questId) {
        if (assetId == null) return;

        Map<UUID, Set<UUID>> byPlayer = index.get(assetId);
        if (byPlayer == null) return;

        Set<UUID> questIds = byPlayer.get(playerId);
        if (questIds != null) questIds.remove(questId);
    }

    @Nonnull
    private static Set<UUID> get(@Nonnull Map<String, Map<UUID, Set<UUID>>> index, @Nullable String assetId, @Nonnull UUID playerId) {
        if (assetId == null) return Set.of();

        return index.getOrDefault(assetId, Map.of()).getOrDefault(playerId, Set.of());
    }
}
