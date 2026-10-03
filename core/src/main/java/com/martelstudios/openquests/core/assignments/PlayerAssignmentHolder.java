package com.martelstudios.openquests.core.assignments;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * One player, whose records travel with their own: read whole on connection, written whole on
 * departure, by the one server they are on. No other server can write them meanwhile.
 */
public final class PlayerAssignmentHolder implements AssignmentHolder {

    @Nonnull
    private final UUID playerId;

    @Nonnull
    private final QuestStoreComponent questStore;

    public PlayerAssignmentHolder(@Nonnull UUID playerId, @Nonnull QuestStoreComponent questStore) {
        this.playerId = playerId;
        this.questStore = questStore;
    }

    /**
     * @return the key a player is known by among holders, for those not holding one at hand.
     */
    @Nonnull
    public static String keyOf(@Nonnull UUID playerId) {
        return "player:" + playerId;
    }

    @Nonnull
    @Override
    public String getKey() {
        return keyOf(playerId);
    }

    @Nullable
    @Override
    public AssignmentRecord getRecord(@Nonnull String assignmentId, @Nonnull String questAssetId) {
        return questStore.getAssignments().get(assignmentId, questAssetId);
    }

    @Nonnull
    @Override
    public Collection<AbstractQuestProgression<?>> getQuests() {
        List<AbstractQuestProgression<?>> quests = new ArrayList<>();
        for (UUID questId : new ArrayList<>(questStore.getQuestIds())) {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
            if (quest != null) quests.add(quest);
        }
        return quests;
    }

    @Override
    public boolean isRunning(@Nonnull AbstractQuestProgression<?> quest) {
        return QuestProgressionService.get().getLiveQuest(quest.getId()) != null && quest.getPlayers().contains(playerId);
    }

    /**
     * Written down only once the player took it, so a quest their constraints refuse is offered
     * again next time.
     */
    @Override
    public boolean handOut(@Nonnull AbstractQuestProgression<?> quest, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next) {
        if (!QuestProgressionService.get().assignQuest(quest, playerId)) return false;

        questStore.getAssignments().put(assignmentId, questAssetId, next);
        questStore.markDirty();
        return true;
    }
}
