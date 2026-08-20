package com.martelstudios.hyquests.core.assignment.services;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.HyQuestCorePlugin;
import com.martelstudios.hyquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.hyquests.core.assignment.conditions.QuestAssignmentCondition;
import com.martelstudios.hyquests.core.assignment.models.QuestAssignmentProgress;
import com.martelstudios.hyquests.core.assignment.stores.QuestAssignmentStoreComponent;
import com.martelstudios.hyquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * The assignments every player is offered on connection. Nothing is materialised: the asset stands
 * in for the assignment, and only the conditions already met are kept on the player.
 */
public class QuestAutoAssignmentService {
    public QuestAutoAssignmentService(JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::handlePlayerConnectEvent);
    }

    public static QuestAutoAssignmentService get() {
        return HyQuestCorePlugin.get().getQuestAutoAssignmentService();
    }

    /**
     * The one pass over the catalog, offering whatever the player has never been offered. Each asset
     * is evaluated on the spot, as it is added, and left to its resolvers afterwards.
     */
    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        var assignmentStore = playerConnectEvent.getHolder()
                                                .ensureAndGetComponent(QuestAssignmentStoreComponent.getComponentType());
        UUID playerId = playerConnectEvent.getPlayerRef().getUuid();

        try (var _ = EntityComponents.cache()) {
            for (QuestAssignmentAsset asset : QuestAssignmentAsset.getAssetMap().getAssetMap().values()) {
                if (!asset.isAutoAssign()) continue;
                if (assignmentStore.getSatisfiedAssignments().contains(asset.getId())) continue;
                if (assignmentStore.getAutoProgress().containsKey(asset.getId())) continue;

                var progress = new QuestAssignmentProgress(asset.getId());
                assignmentStore.getAutoProgress().put(asset.getId(), progress);

                if (progress.evaluate(asset, playerId)) completeAssignment(asset, assignmentStore, playerId);
            }
        }
    }

    /**
     * Relays a resolver's finding for one asset. Called through
     * {@link QuestAssignmentService#setAssignmentConditionSatisfied}, which is the single entry point resolvers use.
     */
    public void completeAssignmentCondition(@Nonnull QuestAssignmentAsset asset, @Nonnull QuestAssignmentCondition<?> condition, @Nonnull UUID playerId) {
        EntityComponents.update(playerId, components -> {
            QuestAssignmentStoreComponent assignmentStore = components.ensureAndGetComponent(QuestAssignmentStoreComponent.getComponentType());

            QuestAssignmentProgress progress = assignmentStore.getAutoProgress(asset.getId());
            if (progress == null) return;

            progress.setConditionSatisfied(asset, condition);
            if (progress.evaluate(asset, playerId)) completeAssignment(asset, assignmentStore, playerId);
        });
    }

    /**
     * Dropping the progress is what takes the asset off the work list.
     */
    private void completeAssignment(@Nonnull QuestAssignmentAsset asset, @Nonnull QuestAssignmentStoreComponent assignmentStore, @Nonnull UUID playerId) {
        assignmentStore.getSatisfiedAssignments().add(asset.getId());
        assignmentStore.clearAutoProgress(asset.getId());

        QuestAssignmentService.get().assignQuests(asset, List.of(playerId));
    }
}
