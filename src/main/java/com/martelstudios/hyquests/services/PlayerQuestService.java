package com.martelstudios.hyquests.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.HyQuestsPlugin;
import com.martelstudios.hyquests.events.QuestAssignedToPlayerEvent;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Keeps a player's quest index in sync. Meant to be driven by {@link AbstractQuest#addPlayer} and
 * {@link AbstractQuest#removePlayer}, which own the other half of the relation: calling these
 * methods directly leaves the quest's own player list stale.
 */
public class PlayerQuestService {

    public static PlayerQuestService get() {
        return HyQuestsPlugin.get().playerQuestService;
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

    /**
     * Fires {@link QuestAssignedToPlayerEvent} only when the quest was not already indexed, so
     * handlers do not re-run on every world re-entry.
     */
    public void addQuestToPlayerStore(@Nonnull QuestStoreComponent playerStore, @Nonnull UUID questId, @Nonnull UUID playerId) {
        AbstractQuest<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest == null) return;

        if (!playerStore.questsRecord.register(questId)) return;

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestAssignedToPlayerEvent.class, playerId)
                    .dispatch(new QuestAssignedToPlayerEvent(quest, playerId));
    }

    /**
     * Must be called on the world thread the given ref belongs to (e.g. inside {@link World#execute}).
     */
    public void removeQuestFromPlayerStore(@Nonnull UUID questId, @Nonnull Ref<EntityStore> ref) {
        ref.getStore()
           .ensureAndGetComponent(ref, QuestStoreComponent.getComponentType()).questsRecord.unregister(questId);
    }

    public void removeQuestFromPlayerStore(@Nonnull UUID questId, @Nonnull Holder<EntityStore> holder) {
        holder.ensureAndGetComponent(QuestStoreComponent.getComponentType()).questsRecord.unregister(questId);
    }
}
