package com.martelstudios.hyquests.services;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.hyquests.HyQuestsPlugin;
import com.martelstudios.hyquests.assets.QuestAsset;
import com.martelstudios.hyquests.events.QuestCompletedEvent;
import com.martelstudios.hyquests.events.QuestRegisteredEvent;
import com.martelstudios.hyquests.events.QuestUnregisteredEvent;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.stores.QuestProgressionStore;
import com.martelstudios.hyquests.stores.QuestStoreComponent;
import com.martelstudios.hyquests.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;

/**
 * Owns the lifecycle of quest instances: registration, progression, completion and rewards.
 * Every instance lives in the single {@link QuestProgressionStore}, and {@link AbstractQuest#getPlayers()}
 * is the source of truth for who holds it — {@link QuestStoreComponent} being the reverse index
 * used to know what to load.
 * <p>
 * Quests are agnostic of scope: {@code UniverseQuestService} and {@code WorldQuestService} assign
 * and unassign them on the events that concern them.
 */
public class QuestProgressionService {
    private final QuestProgressionStore dataStore;

    public QuestProgressionService(QuestProgressionStore questProgressionStore) {
        this.dataStore = questProgressionStore;
    }

    public static QuestProgressionService get() {
        return HyQuestsPlugin.get().getQuestProgressionService();
    }

    /**
     * Registers a concrete quest type's polymorphic serialization tag, for both its asset and
     * its runtime form, under the same id. Mirrors {@code ObjectivePlugin.registerTask}.
     */
    public <Q extends AbstractQuest<Q>, QAsset extends QuestAsset> void registerQuestType(String id, Class<QAsset> questAssetClass, BuilderCodec<QAsset> questAssetCodec, Class<Q> questClass, BuilderCodec<Q> questCodec) {
        QuestAsset.CODEC.register(id, questAssetClass, questAssetCodec);
        AbstractQuest.CODEC.register(id, questClass, questCodec);
    }

    public AbstractQuest<?> loadQuest(UUID questId) {
        return dataStore.load(questId);
    }

    /**
     * Creates and registers a new quest in the store.
     *
     * @param questAsset the quest asset to create the quest from
     */
    public AbstractQuest<?> registerQuest(@Nonnull QuestAsset questAsset) {
        var quest = questAsset.create();
        quest.markDirty();
        registerQuest(quest);
        return quest;
    }

    /**
     * Registers a new quest in the store, so it gets persisted.
     *
     * @param quest the quest to register
     */
    public void registerQuest(@Nonnull AbstractQuest<?> quest) {
        dataStore.add(quest);
        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestRegisteredEvent.class, quest.getId())
                    .dispatch(new QuestRegisteredEvent(quest));
    }

    /**
     * Unregisters a quest from the store and deletes its persisted file. Clean up all references left in any player/world/universe index.
     *
     * @param questId the id of the quest to unregister
     */
    public AbstractQuest<?> unregisterQuest(@Nonnull UUID questId) {
        AbstractQuest<?> quest = dataStore.get(questId);
        if (quest == null) return null;

        AbstractQuest<?> removed = dataStore.removeAndDeleteFromDisk(questId);

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestUnregisteredEvent.class, quest.getId())
                    .dispatch(new QuestUnregisteredEvent(quest));

        return removed;
    }

    /**
     * Called by {@link AbstractQuest#update} when a quest reaches a terminal state: archives it
     * for every assigned player, then unregisters the instance.
     *
     * @param questId the id of the quest that just completed
     */
    public AbstractQuest<?> completeQuest(@Nonnull UUID questId) {
        AbstractQuest<?> quest = getQuest(questId);
        if (quest == null) return null;

        unregisterQuest(questId);

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestCompletedEvent.class, questId)
                    .dispatch(new QuestCompletedEvent(quest));

        return quest;
    }

    public AbstractQuest<?> getQuest(UUID questId) {
        return dataStore.get(questId);
    }

    /**
     * Progresses every quest of the visitor's type on the server. Prefer the overload taking a
     * player's own quest ids: a visitor built for one player discards all the others anyway.
     */
    public <Q extends AbstractQuest<Q>> void progress(@Nonnull QuestVisitor<Q> visitor) {
        progress(visitor, dataStore.getForType(visitor.getQuestType()));
    }

    /**
     * Progresses only the given quests, skipping those of another type. The collection may be a
     * live index: completing a quest unregisters it, and the sets involved tolerate that.
     */
    @SuppressWarnings("unchecked")
    public <Q extends AbstractQuest<Q>> void progress(@Nonnull QuestVisitor<Q> visitor, @Nonnull Collection<UUID> questIds) {
        Class<Q> questType = visitor.getQuestType();

        for (UUID id : questIds) {
            AbstractQuest<?> quest = dataStore.get(id);
            if (!questType.isInstance(quest)) continue;

            ((Q) quest).update(visitor);
        }
    }
}
