package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.IEventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.scopes.ScopeServices;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.utils.Players;
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
    private final QuestProgressionService progression;
    private final ScopeServices scopes;
    private final Players players;

    public WorldClosingService(@Nonnull QuestStorage storage, @Nonnull QuestProgressionService progression, @Nonnull ScopeServices scopes, @Nonnull Players players) {
        this.storage = storage;
        this.progression = progression;
        this.scopes = scopes;
        this.players = players;
    }

    /**
     * Subscribes to world removals.
     */
    public void listen(@Nonnull IEventRegistry events) {
        // Last, so a removal another plugin called off is seen as such
        events.registerGlobal(EventPriority.LAST, RemoveWorldEvent.class, this::handleRemoveWorldEvent);
    }

    private void handleRemoveWorldEvent(@Nonnull RemoveWorldEvent removeWorldEvent) {
        if (removeWorldEvent.isCancelled()) return;
        if (removeWorldEvent.getRemovalReason() != RemoveWorldEvent.RemovalReason.GENERAL) return;

        World world = removeWorldEvent.getWorld();

        scopes.worlds().setClosing(world, true);
        try {
            for (UUID questId : new ArrayList<>(scopes.worlds().getQuestIds(world))) {
                AbstractQuestProgression<?> quest = progression.loadQuest(questId);
                if (quest != null) close(world, quest);
            }

            scopes.worlds().deleteIndex(world);
            storage.deleteAssignments(WorldQuestService.indexKey(world));
            scopes.worldGroups().forgetWorld(world.getWorldConfig().getUuid());

            LOGGER.atInfo().log("Closed the quests of world %s", world.getName());
        } finally {
            scopes.worlds().setClosing(world, false);
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
        if (scope != null && scope.outlives(scopes, world.getWorldConfig().getUuid())) {
            scopes.worlds().removeQuestHere(world, questId);
            return;
        }

        if (progression.getLiveQuest(questId) != null) {
            progression.progress(new SetStateVisitor(QuestState.FAILED), List.of(questId));
        }

        if (quest.getPlayers().isEmpty() && quest.getAbandonedPlayers().isEmpty()) {
            progression.unregisterQuest(quest);
        } else if (!isHeldOnline(quest)) {
            progression.unloadQuest(questId);
        }
    }

    private boolean isHeldOnline(@Nonnull AbstractQuestProgression<?> quest) {
        for (UUID playerId : quest.getPlayers()) {
            if (players.isOnline(playerId)) return true;
        }

        for (UUID playerId : quest.getAbandonedPlayers()) {
            if (players.isOnline(playerId)) return true;
        }

        return false;
    }
}
