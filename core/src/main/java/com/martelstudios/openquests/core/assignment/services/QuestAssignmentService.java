package com.martelstudios.openquests.core.assignment.services;

import com.martelstudios.openquests.core.OpenQuestCorePlugin;
import com.martelstudios.openquests.core.assets.QuestAsset;
import com.martelstudios.openquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.openquests.core.assignment.conditions.QuestAssignmentCondition;
import com.martelstudios.openquests.core.assignment.models.QuestAssignment;
import com.martelstudios.openquests.core.assignment.stores.QuestAssignmentStore;
import com.martelstudios.openquests.core.assignment.stores.QuestAssignmentStoreComponent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

/**
 * Handles the assignments a player was explicitly offered, private or shared. The catalog handed
 * to everyone lives in {@link QuestAutoAssignmentService}, which shares this one's quest handover.
 */
public class QuestAssignmentService {

    private final QuestAssignmentStore assignmentStore;

    public QuestAssignmentService(@Nonnull QuestAssignmentStore assignmentStore) {
        this.assignmentStore = assignmentStore;
    }

    public static QuestAssignmentService get() {
        return OpenQuestCorePlugin.get().getQuestAssignmentService();
    }

    /**
     * Offers an assignment explicitly, outside the auto-assign catalog. More than one player makes
     * it shared: they all receive the quests once every one of them satisfies it.
     */
    @Nonnull
    public QuestAssignment addToPlayers(@Nonnull QuestAssignmentAsset asset, @Nonnull UUID... playerIds) {
        QuestAssignment assignment = new QuestAssignment(asset);
        Arrays.asList(playerIds).forEach(assignment::addPlayer);

        // Nothing left to wait on: the quests go out without the assignment ever being stored
        if (isSatisfiedByAllPlayers(assignment)) {
            setAssignmentSatisfied(assignment);
            return assignment;
        }

        if (assignment.isShared()) assignmentStore.add(assignment);

        for (UUID playerId : playerIds) {
            EntityComponents.update(playerId, components -> {
                QuestAssignmentStoreComponent playerAssignments = components.ensureAndGetComponent(QuestAssignmentStoreComponent.getComponentType());
                if (assignment.isShared()) {
                    playerAssignments.addSharedAssignment(assignment);
                } else {
                    playerAssignments.addOwnAssignment(assignment);
                }
            });
        }

        return assignment;
    }

    /**
     * Hands every condition of every asset to its resolver, so events can later name the few
     * assignments worth re-checking instead of the whole catalog.
     */
    public void registerAllAssignmentsToItsConditions() {
        for (QuestAssignmentAsset asset : QuestAssignmentAsset.getAssetMap().getAssetMap().values()) {
            for (var condition : asset.getConditions()) {
                condition.register(asset);
            }
        }
    }

    /**
     * Set for the given player the given condition as satisfied for all {@link QuestAssignment} of the given {@link QuestAssignmentAsset}.
     */
    public void setAssignmentConditionSatisfied(@Nonnull QuestAssignmentAsset asset, @Nonnull QuestAssignmentCondition<?> condition, @Nonnull UUID playerId) {
        QuestAutoAssignmentService.get().completeAssignmentCondition(asset, condition, playerId);

        EntityComponents.update(playerId, components -> {
            QuestAssignmentStoreComponent assignmentStore = components.ensureAndGetComponent(QuestAssignmentStoreComponent.getComponentType());
            for (QuestAssignment assignment : assignmentStore.getAssignmentsOf(asset)) {
                // On a shared assignment this player's word settles nothing for the others
                if (!assignment.isShared()) assignment.setConditionSatisfied(condition);

                if (isSatisfiedByAllPlayers(assignment)) setAssignmentSatisfied(assignment);
            }
        });
    }

    /**
     * Nobody is carried by the group: a shared assignment waits for every member. Caching is left
     * to the private case, where the only player is the one being checked.
     */
    private boolean isSatisfiedByAllPlayers(@Nonnull QuestAssignment assignment) {
        boolean enableUseCache = !assignment.isShared();

        try (var _ = EntityComponents.cache()) {
            for (UUID playerId : assignment.getPlayers()) {
                if (!assignment.evaluate(playerId, enableUseCache)) return false;
            }
        }

        return true;
    }

    /**
     * Registers the quests and puts every player on them. Shared with the auto-assign catalog,
     * which reaches the same outcome from a different bookkeeping.
     */
    public void assignQuests(@Nonnull QuestAssignmentAsset asset, @Nonnull Collection<UUID> playerIds) {
        for (String questAssetId : asset.getQuestAssetIds()) {
            QuestAsset questAsset = QuestAsset.getAsset(questAssetId);
            if (questAsset == null) continue;

            var quest = QuestProgressionService.get().registerQuest(questAsset);
            for (UUID playerId : playerIds) {
                quest.addPlayer(playerId);
            }
        }
    }

    private void setAssignmentSatisfied(@Nonnull QuestAssignment assignment) {
        QuestAssignmentAsset asset = assignment.getAsset();
        if (asset == null) return;

        Collection<UUID> playerIds = assignment.getPlayers();
        assignQuests(asset, playerIds);

        for (UUID playerId : playerIds) {
            EntityComponents.update(playerId, components -> {
                QuestAssignmentStoreComponent assignmentStore = components.ensureAndGetComponent(QuestAssignmentStoreComponent.getComponentType());
                assignmentStore.removeAssignment(assignment.getId());
                assignmentStore.getSatisfiedAssignments().add(assignment.getAssetId());
            });
        }

        if (assignment.isShared()) assignmentStore.removeAndDeleteFromDisk(assignment.getId());
    }
}
