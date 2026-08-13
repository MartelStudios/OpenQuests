package com.martelstudios.hyquests.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.HyQuestsPlugin;
import com.martelstudios.hyquests.events.QuestAssignedToPlayerEvent;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;

public class PlayerQuestService {
    private static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PlayerQuestService() {
    }

    public static PlayerQuestService get() {
        return HyQuestsPlugin.get().playerQuestService;
    }


    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void addQuestToPlayerStore(UUID questId, Collection<PlayerRef> playerRefs) {
        playerRefs.stream()
                  .filter(ref -> ref.getReference() != null)
                  .forEach(ref -> addQuestToPlayerStore(questId, ref.getReference()));
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
        var playerStoreComponent = holder.ensureAndGetComponent(QuestStoreComponent.getComponentType());
        var playerId = holder.getComponent(UUIDComponent.getComponentType()).getUuid();
        addQuestToPlayerStore(playerStoreComponent, questId, playerId);
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void addQuestsToPlayerStore(@Nonnull Collection<UUID> questIds, @Nonnull Ref<EntityStore> ref) {
        var playerStoreComponent = ref.getStore().ensureAndGetComponent(ref, QuestStoreComponent.getComponentType());
        var playerId = ref.getStore().getComponent(ref, UUIDComponent.getComponentType()).getUuid();
        questIds.forEach(questId -> addQuestToPlayerStore(playerStoreComponent, questId, playerId));
    }

    public void addQuestsToPlayerStore(@Nonnull Collection<UUID> questIds, @Nonnull Holder<EntityStore> holder) {
        var playerStoreComponent = holder.ensureAndGetComponent(QuestStoreComponent.getComponentType());
        var playerId = holder.getComponent(UUIDComponent.getComponentType()).getUuid();
        questIds.forEach(questId -> addQuestToPlayerStore(playerStoreComponent, questId, playerId));
    }

    public void addQuestToPlayerStore(QuestStoreComponent playerStore, UUID questId, UUID playerId) {
        AbstractQuest<?> quest = QuestProgressionService.get().getQuest(questId);

        if (quest == null) return;

        playerStore.questsRecord.register(questId);

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestAssignedToPlayerEvent.class, playerId)
                    .dispatch(new QuestAssignedToPlayerEvent(quest, playerId));
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(UUID questId, Collection<PlayerRef> playerRefs) {
        playerRefs.stream()
                  .filter(ref -> ref.getReference() != null)
                  .forEach(ref -> removeQuestFromPlayerStore(questId, ref.getReference()));
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull UUID questId, @Nonnull Ref<EntityStore> ref) {
        ref.getStore()
           .ensureAndGetComponent(ref, QuestStoreComponent.getComponentType()).questsRecord.unregister(questId);
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull UUID questId, @Nonnull Holder<EntityStore> holder) {
        holder.ensureAndGetComponent(QuestStoreComponent.getComponentType()).questsRecord.unregister(questId);
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull Collection<UUID> questIds, @Nonnull Ref<EntityStore> ref) {
        var playerStore = ref.getStore().ensureAndGetComponent(ref, QuestStoreComponent.getComponentType());
        questIds.forEach(playerStore.questsRecord::unregister);
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull Collection<UUID> questIds, @Nonnull Holder<EntityStore> holder) {
        var playerStore = holder.ensureAndGetComponent(QuestStoreComponent.getComponentType());
        questIds.forEach(playerStore.questsRecord::unregister);
    }

}
