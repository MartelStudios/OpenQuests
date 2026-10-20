package com.martelstudios.openquests.core.scopes.player;

import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.event.IEventRegistry;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.openquests.core.stores.PlayerComponentTypes;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.stores.QuestsRecord;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.utils.Players;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Keeps a player's quest index in sync. Meant to be driven by {@link AbstractQuestProgression#addPlayer} and
 * {@link AbstractQuestProgression#removePlayer}, which own the other half of the relation: calling these
 * methods directly leaves the quest's own player list stale.
 *
 * <p>Only the index. Reading a player's quests back and writing them out is
 * {@link com.martelstudios.openquests.core.services.QuestPlayerStateService}'s, which is where the
 * storage is spoken to.
 */
public class PlayerQuestService {

    private final IEventBus eventBus;
    private final Players players;
    private final PlayerComponentTypes components;

    public PlayerQuestService(@Nonnull IEventBus events, @Nonnull Players players, @Nonnull PlayerComponentTypes components) {
        this.eventBus = events;
        this.players = players;
        this.components = components;
    }

    /**
     * Subscribes to the quest membership changes mirrored into each player's record.
     */
    public void listen(@Nonnull IEventRegistry events) {
        events.registerGlobal(QuestPlayerAddedEvent.class, this::handleQuestPlayerAddedEvent);
        events.registerGlobal(QuestPlayerRemovedEvent.class, this::handleQuestPlayerRemovedEvent);
        events.registerGlobal(QuestUnregisteredEvent.class, this::handleQuestUnregisteredEvent);
    }

    public static PlayerQuestService get() {
        return OpenQuestsServices.installed().getPlayerQuestService();
    }

    /**
     * @return the quest index of a player, created if they have none yet.
     */
    @Nonnull
    public QuestsRecord getQuests(@Nonnull EntityComponents playerComponents) {
        return playerComponents.ensureAndGetComponent(components.questStore()).getQuests();
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
        players.update(playerId, player -> addQuestToPlayerStore(player.ensureAndGetComponent(components.questStore()), quest, playerId));
    }

    public void addQuestToPlayerStore(@Nonnull QuestStoreComponent playerStore, @Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        if (!playerStore.getQuests().register(quest.getId())) return;

        playerStore.markDirty();

        eventBus.dispatchFor(QuestAddedToPlayerStoreEvent.class, playerId).dispatch(new QuestAddedToPlayerStoreEvent(quest, playerId));
    }

    public void removeQuestFromPlayerStore(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        players.update(playerId, player -> removeQuestFromPlayerStore(player.ensureAndGetComponent(components.questStore()), quest, playerId));
    }

    /**
     * Takes the quest rather than its id: it is unregistered by the time this runs on the player's
     * world thread, so looking it back up would find nothing and leave a stale id behind.
     */
    public void removeQuestFromPlayerStore(@Nonnull QuestStoreComponent playerStore, @Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID playerId) {
        if (!playerStore.getQuests().unregister(quest.getId())) return;

        playerStore.markDirty();

        eventBus.dispatchFor(QuestRemovedFromPlayerStoreEvent.class, playerId).dispatch(new QuestRemovedFromPlayerStoreEvent(quest, playerId));
    }
}
