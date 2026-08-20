package com.martelstudios.hyquests.core.scopes.player;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.HyQuestCorePlugin;
import com.martelstudios.hyquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.hyquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.hyquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;
import com.martelstudios.hyquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.hyquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.hyquests.core.stores.QuestStoreComponent;
import com.martelstudios.hyquests.core.stores.QuestsRecord;
import com.martelstudios.hyquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Keeps a player's quest index in sync. Meant to be driven by {@link AbstractQuestProgression#addPlayer} and
 * {@link AbstractQuestProgression#removePlayer}, which own the other half of the relation: calling these
 * methods directly leaves the quest's own player list stale.
 */
public class PlayerQuestService {

    public PlayerQuestService(JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::handlePlayerConnectEvent);
        plugin.getEventRegistry().registerGlobal(QuestPlayerAddedEvent.class, this::handleQuestPlayerAddedEvent);
        plugin.getEventRegistry().registerGlobal(QuestPlayerRemovedEvent.class, this::handleQuestPlayerRemovedEvent);
        plugin.getEventRegistry().registerGlobal(QuestUnregisteredEvent.class, this::handleQuestUnregisteredEvent);
    }

    public static PlayerQuestService get() {
        return HyQuestCorePlugin.get().getPlayerQuestService();
    }

    /**
     * @return the quest index of a player, created if they have none yet.
     */
    @Nonnull
    public QuestsRecord getQuests(@Nonnull EntityComponents playerComponents) {
        return playerComponents.ensureAndGetComponent(QuestStoreComponent.getComponentType()).questsRecord;
    }

    /**
     * Pulls the player's own quests into the datastore. Scope services assign theirs separately,
     * each on the event that concerns it.
     */
    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        getQuests(EntityComponents.of(playerConnectEvent.getHolder())).loadAll();
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
    }

    public void addQuestToPlayerStore(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        EntityComponents.update(playerId, components -> addQuestToPlayerStore(components.ensureAndGetComponent(QuestStoreComponent.getComponentType()), quest, playerId));
    }

    public void addQuestToPlayerStore(@Nonnull QuestStoreComponent playerStore, @Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        if (!playerStore.questsRecord.register(quest.getId())) return;

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
        if (!playerStore.questsRecord.unregister(quest.getId())) return;

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestRemovedFromPlayerStoreEvent.class, playerId)
                    .dispatch(new QuestRemovedFromPlayerStoreEvent(quest, playerId));
    }
}
