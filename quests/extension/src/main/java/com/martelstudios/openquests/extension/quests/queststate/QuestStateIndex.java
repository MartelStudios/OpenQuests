package com.martelstudios.openquests.extension.quests.queststate;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which quest-state quests a player holds that read an asset, so a change to that asset reaches
 * them. A cache of the quest store rather than an authority: everything here is put back by the
 * store's own events.
 */
public class QuestStateIndex {

    /**
     * Watched asset id, then player, then the quest-state quests reading it.
     */
    private final Map<String, Map<UUID, Set<UUID>>> watchers = new ConcurrentHashMap<>();

    public void track(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        add(watchers, watchedAssetOf(quest), playerId, quest.getId());
    }

    public void forget(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        remove(watchers, watchedAssetOf(quest), playerId, quest.getId());
    }

    /**
     * @return the quest-state quests this player holds that read the asset's outcome.
     */
    @Nonnull
    public Set<UUID> watchersOf(@Nullable String assetId, @Nonnull UUID playerId) {
        return get(watchers, assetId, playerId);
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
