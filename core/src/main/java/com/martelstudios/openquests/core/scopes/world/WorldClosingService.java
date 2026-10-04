package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.SetStateVisitor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Settles what a world holds as it closes for good, an instance done with or a world removed by
 * hand: each quest's scope says whether it goes on elsewhere; the others fail. A crash keeps
 * everything, the world being reloaded, and so does a server stop, which raises no removal at all.
 */
public class WorldClosingService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final QuestStorage storage;

    public WorldClosingService(@Nonnull JavaPlugin plugin, @Nonnull QuestStorage storage) {
        this.storage = storage;

        // Last, so a removal another plugin called off is seen as such
        plugin.getEventRegistry().registerGlobal(EventPriority.LAST, RemoveWorldEvent.class, this::handleRemoveWorldEvent);
    }

    private void handleRemoveWorldEvent(@Nonnull RemoveWorldEvent removeWorldEvent) {
        if (removeWorldEvent.isCancelled()) return;
        if (removeWorldEvent.getRemovalReason() != RemoveWorldEvent.RemovalReason.GENERAL) return;

        World world = removeWorldEvent.getWorld();
        WorldQuestStoreResource store = WorldQuestService.getWorldQuestStoreFromWorld(world);

        store.setClosing(true);
        try {
            for (UUID questId : new ArrayList<>(WorldQuestService.get().getQuestIds(world))) {
                AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
                if (quest != null) close(world, quest);
            }

            WorldQuestService.get().deleteIndex(world);
            storage.deleteAssignments(WorldQuestService.indexKey(world));
            WorldGroupIndex.get().forgetWorld(world.getWorldConfig().getUuid());

            LOGGER.atInfo().log("Closed the quests of world %s", world.getName());
        } finally {
            store.setClosing(false);
        }
    }

    /**
     * A quest its scope keeps going only leaves this world. Otherwise one still running fails, and
     * one no journal holds is done away with, nothing being left to ever read it; the rest stays
     * with whoever holds it, in memory only while one of them is online.
     */
    private void close(@Nonnull World world, @Nonnull AbstractQuestProgression<?> quest) {
        UUID questId = quest.getId();

        QuestScope scope = quest.getScope();
        if (scope != null && scope.outlives(world.getWorldConfig().getUuid())) {
            WorldQuestService.get().removeQuestHere(world, questId);
            return;
        }

        if (QuestProgressionService.get().getLiveQuest(questId) != null) {
            QuestProgressionService.get().progress(new SetStateVisitor(QuestState.FAILED), List.of(questId));
        }

        if (quest.getPlayers().isEmpty() && quest.getAbandonedPlayers().isEmpty()) {
            QuestProgressionService.get().unregisterQuest(quest);
        } else if (!isHeldOnline(quest)) {
            QuestProgressionService.get().unloadQuest(questId);
        }
    }

    private static boolean isHeldOnline(@Nonnull AbstractQuestProgression<?> quest) {
        for (UUID playerId : quest.getPlayers()) {
            if (Universe.get().getPlayer(playerId) != null) return true;
        }

        for (UUID playerId : quest.getAbandonedPlayers()) {
            if (Universe.get().getPlayer(playerId) != null) return true;
        }

        return false;
    }
}
