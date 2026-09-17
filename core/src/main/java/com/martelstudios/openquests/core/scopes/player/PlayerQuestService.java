package com.martelstudios.openquests.core.scopes.player;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.stores.QuestsRecord;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps a player's quest index in sync. Meant to be driven by {@link AbstractQuestProgression#addPlayer} and
 * {@link AbstractQuestProgression#removePlayer}, which own the other half of the relation: calling these
 * methods directly leaves the quest's own player list stale.
 */
public class PlayerQuestService {

    public PlayerQuestService(JavaPlugin plugin) {
        // EventPriority.FIRST, so that the quests kept with the player are back in the store before
        // any scope resolves an id against it and takes a miss for a quest that no longer exists
        plugin.getEventRegistry().registerGlobal(EventPriority.FIRST, PlayerConnectEvent.class, this::handlePlayerConnectEvent);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, this::handlePlayerDisconnectEvent);
        plugin.getEventRegistry().registerGlobal(QuestPlayerAddedEvent.class, this::handleQuestPlayerAddedEvent);
        plugin.getEventRegistry().registerGlobal(QuestPlayerRemovedEvent.class, this::handleQuestPlayerRemovedEvent);
        plugin.getEventRegistry().registerGlobal(QuestUnregisteredEvent.class, this::handleQuestUnregisteredEvent);
    }

    public static PlayerQuestService get() {
        return OpenQuestsCorePlugin.get().getPlayerQuestService();
    }

    /**
     * @return the quest index of a player, created if they have none yet.
     */
    @Nonnull
    public QuestsRecord getQuests(@Nonnull EntityComponents playerComponents) {
        return playerComponents.ensureAndGetComponent(QuestStoreComponent.getComponentType()).getQuests();
    }

    /**
     * The quests kept with the player are already decoded: they only have to enter the store. The
     * rest is loaded from its own files afterwards.
     */
    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        var questStore = playerConnectEvent.getHolder().ensureAndGetComponent(QuestStoreComponent.getComponentType());

        for (AbstractQuestProgression<?> quest : questStore.getOwnQuests().values()) {
            QuestProgressionService.get().registerLoadedQuest(quest);
        }

        questStore.loadQuests();
    }

    /**
     * Gives back what this player's session had brought in. Without it the store only ever grows:
     * every quest of everyone who ever connected stays loaded until the server stops.
     */
    private void handlePlayerDisconnectEvent(@Nonnull PlayerDisconnectEvent playerDisconnectEvent) {
        PlayerRef playerRef = playerDisconnectEvent.getPlayerRef();

        EntityComponents components = EntityComponents.of(playerRef);
        if (components == null) return;

        QuestStoreComponent questStore = components.getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        for (UUID questId : questStore.getQuestIds()) {
            unloadIfUnheld(questId, playerRef.getUuid());
        }
    }

    /**
     * Keeps a quest someone else is still playing, and lets the rest go.
     */
    private void unloadIfUnheld(@Nonnull UUID questId, @Nonnull UUID leavingPlayerId) {
        AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest == null) return;

        if (isAnyOnline(quest.getPlayers(), leavingPlayerId)) return;
        if (isAnyOnline(quest.getAbandonedPlayers(), leavingPlayerId)) return;

        QuestProgressionService.get().unloadQuest(questId);
    }

    /**
     * @param leavingPlayerId discounted, being still listed among the online while their own
     * departure is announced.
     */
    private static boolean isAnyOnline(@Nonnull Set<UUID> playerIds, @Nonnull UUID leavingPlayerId) {
        for (UUID playerId : playerIds) {
            if (!playerId.equals(leavingPlayerId) && Universe.get().getPlayer(playerId) != null) return true;
        }

        return false;
    }

    private void handleQuestPlayerAddedEvent(@Nonnull QuestPlayerAddedEvent questPlayerAddedEvent) {
        addQuestToPlayerStore(questPlayerAddedEvent.getQuest(), questPlayerAddedEvent.getPlayerId());
    }

    private void handleQuestPlayerRemovedEvent(@Nonnull QuestPlayerRemovedEvent questPlayerRemovedEvent) {
        removeQuestFromPlayerStore(questPlayerRemovedEvent.getQuest(), questPlayerRemovedEvent.getPlayerId());
    }

    /**
     * A quest leaving the store has to leave every holder's index too, and none of them was
     * removed from it individually.
     */
    private void handleQuestUnregisteredEvent(@Nonnull QuestUnregisteredEvent questUnregisteredEvent) {
        AbstractQuestProgression<?> quest = questUnregisteredEvent.getQuest();

        for (UUID playerId : quest.getPlayers()) {
            removeQuestFromPlayerStore(quest, playerId);
        }

        for (UUID playerId : quest.getAbandonedPlayers()) {
            removeQuestFromPlayerStore(quest, playerId);
        }
    }

    public void addQuestToPlayerStore(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        EntityComponents.update(playerId, components -> addQuestToPlayerStore(components.ensureAndGetComponent(QuestStoreComponent.getComponentType()), quest, playerId));
    }

    public void addQuestToPlayerStore(@Nonnull QuestStoreComponent playerStore, @Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        if (!playerStore.getQuests().register(quest.getId())) return;

        playerStore.addOwnQuest(quest);

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestAddedToPlayerStoreEvent.class, playerId)
                    .dispatch(new QuestAddedToPlayerStoreEvent(quest, playerId));
    }

    public void removeQuestFromPlayerStore(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        EntityComponents.update(playerId, components -> removeQuestFromPlayerStore(components.ensureAndGetComponent(QuestStoreComponent.getComponentType()), quest, playerId));
    }

    /**
     * Takes the quest rather than its id: it is unregistered by the time this runs on the player's
     * world thread, so looking it back up would find nothing and leave a stale id behind.
     */
    public void removeQuestFromPlayerStore(@Nonnull QuestStoreComponent playerStore, @Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        if (!playerStore.getQuests().unregister(quest.getId())) return;

        playerStore.removeOwnQuest(quest.getId());

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestRemovedFromPlayerStoreEvent.class, playerId)
                    .dispatch(new QuestRemovedFromPlayerStoreEvent(quest, playerId));
    }
}
