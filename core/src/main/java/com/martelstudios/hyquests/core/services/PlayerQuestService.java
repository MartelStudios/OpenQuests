package com.martelstudios.hyquests.core.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.core.HyQuestCorePlugin;
import com.martelstudios.hyquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.hyquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.hyquests.core.models.AbstractQuest;
import com.martelstudios.hyquests.core.stores.QuestStoreComponent;
import com.martelstudios.hyquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Keeps a player's quest index in sync. Meant to be driven by {@link AbstractQuest#addPlayer} and
 * {@link AbstractQuest#removePlayer}, which own the other half of the relation: calling these
 * methods directly leaves the quest's own player list stale.
 */
public class PlayerQuestService {

    public static PlayerQuestService get() {
        return HyQuestCorePlugin.get().getPlayerQuestService();
    }

    /**
     * @return the quest index of a live player, created if they have none yet.
     */
    @Nonnull
    public QuestsRecord getQuests(@Nonnull Ref<EntityStore> ref) {
        return ref.getStore().ensureAndGetComponent(ref, QuestStoreComponent.getComponentType()).questsRecord;
    }

    /**
     * @return the quest index of a player loaded from storage, created if they have none yet.
     */
    @Nonnull
    public QuestsRecord getQuests(@Nonnull Holder<EntityStore> holder) {
        return holder.ensureAndGetComponent(QuestStoreComponent.getComponentType()).questsRecord;
    }

    /**
     * Pulls the player's own quests into the datastore. Scope services assign theirs separately,
     * each on the event that concerns it.
     */
    public void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        getQuests(playerConnectEvent.getHolder()).loadAll();
    }

    public void addQuestToPlayerStore(@Nonnull UUID questId, @Nonnull UUID playerId) {
        PlayerRef online = Universe.get().getPlayer(playerId);
        if (online != null) {
            var ref = online.getReference();
            if (ref != null) {
                var world = ref.getStore().getExternalData().getWorld();
                world.execute(() -> addQuestToPlayerStore(questId, ref));
            }
        } else {
            Universe.get().getPlayerStorage().update(playerId, holder -> addQuestToPlayerStore(questId, holder));
        }
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void addQuestToPlayerStore(@Nonnull UUID questId, @Nonnull Ref<EntityStore> ref) {
        var questStoreComponent = ref.getStore().ensureAndGetComponent(ref, QuestStoreComponent.getComponentType());
        var playerId = ref.getStore().getComponent(ref, UUIDComponent.getComponentType()).getUuid();
        addQuestToPlayerStore(questStoreComponent, questId, playerId);
    }

    public void addQuestToPlayerStore(@Nonnull UUID questId, @Nonnull Holder<EntityStore> holder) {
        var questStoreComponent = holder.ensureAndGetComponent(QuestStoreComponent.getComponentType());
        var playerId = holder.getComponent(UUIDComponent.getComponentType()).getUuid();
        addQuestToPlayerStore(questStoreComponent, questId, playerId);
    }

    public void addQuestToPlayerStore(@Nonnull QuestStoreComponent playerStore, @Nonnull UUID questId, @Nonnull UUID playerId) {
        AbstractQuest<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest == null) return;

        if (!playerStore.questsRecord.register(questId)) return;

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestPlayerAddedEvent.class, playerId)
                    .dispatch(new QuestPlayerAddedEvent(quest, playerId));
    }

    public void removeQuestFromPlayerStore(@Nonnull UUID questId, @Nonnull UUID playerId) {
        PlayerRef online = Universe.get().getPlayer(playerId);
        if (online != null) {
            var ref = online.getReference();
            if (ref != null) {
                var world = ref.getStore().getExternalData().getWorld();
                world.execute(() -> removeQuestFromPlayerStore(questId, ref));
            }
        } else {
            Universe.get().getPlayerStorage().update(playerId, holder -> removeQuestFromPlayerStore(questId, holder));
        }
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull UUID questId, @Nonnull Ref<EntityStore> ref) {
        var questStoreComponent = ref.getStore().ensureAndGetComponent(ref, QuestStoreComponent.getComponentType());
        var playerId = ref.getStore().getComponent(ref, UUIDComponent.getComponentType()).getUuid();
        removeQuestFromPlayerStore(questStoreComponent, questId, playerId);
    }

    public void removeQuestFromPlayerStore(@Nonnull UUID questId, @Nonnull Holder<EntityStore> holder) {
        var questStoreComponent = holder.ensureAndGetComponent(QuestStoreComponent.getComponentType());
        var playerId = holder.getComponent(UUIDComponent.getComponentType()).getUuid();
        removeQuestFromPlayerStore(questStoreComponent, questId, playerId);
    }

    public void removeQuestFromPlayerStore(@Nonnull QuestStoreComponent playerStore, @Nonnull UUID questId, @Nonnull UUID playerId) {
        AbstractQuest<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest == null) return;

        if (!playerStore.questsRecord.unregister(questId)) return;

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestPlayerRemovedEvent.class, playerId)
                    .dispatch(new QuestPlayerRemovedEvent(quest, playerId));
    }
}
