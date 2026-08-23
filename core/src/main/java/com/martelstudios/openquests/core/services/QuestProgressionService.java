package com.martelstudios.openquests.core.services;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.openquests.core.OpenQuestCorePlugin;
import com.martelstudios.openquests.core.assets.QuestAsset;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestRegisteredEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the lifecycle of quest instances: registration, progression, completion and rewards.
 * Every instance lives in the single {@link QuestProgressionStore}, and {@link AbstractQuestProgression#getPlayers()}
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
        return OpenQuestCorePlugin.get().getQuestProgressionService();
    }

    /**
     * Registers a concrete quest type's polymorphic serialization tag, for both its asset and
     * its runtime form, under the same id. Mirrors {@code ObjectivePlugin.registerTask}.
     */
    public <Q extends AbstractQuestProgression<Q>, QAsset extends QuestAsset> void registerQuestType(String id, Class<QAsset> questAssetClass, BuilderCodec<QAsset> questAssetCodec, Class<Q> questClass, BuilderCodec<Q> questCodec) {
        QuestAsset.CODEC.register(id, questAssetClass, questAssetCodec);
        AbstractQuestProgression.CODEC.register(id, questClass, questCodec);
    }

    public AbstractQuestProgression<?> loadQuest(UUID questId) {
        return dataStore.load(questId);
    }

    /**
     * Creates and registers a new quest in the store.
     *
     * @param questAsset the quest asset to create the quest from
     */
    public AbstractQuestProgression<?> registerQuest(@Nonnull QuestAsset questAsset) {
        var quest = questAsset.create();
        quest.markDirty();
        registerQuest(quest);
        return quest;
    }

    /**
     * Puts a quest that was decoded elsewhere — a player's own store — back into the registry,
     * without the registration events: it is not new, it is coming back.
     */
    public void registerLoadedQuest(@Nonnull AbstractQuestProgression<?> quest) {
        dataStore.add(quest);
    }

    /**
     * Registers a new quest in the store, so it gets persisted.
     *
     * @param quest the quest to register
     */
    public void registerQuest(@Nonnull AbstractQuestProgression<?> quest) {
        dataStore.add(quest);
        quest.onRegistered();

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
    public AbstractQuestProgression<?> unregisterQuest(@Nonnull UUID questId) {
        AbstractQuestProgression<?> quest = dataStore.get(questId);
        if (quest == null) return null;

        AbstractQuestProgression<?> removed = dataStore.removeAndDeleteFromDisk(questId);
        quest.onUnregistered();

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestUnregisteredEvent.class, quest.getId())
                    .dispatch(new QuestUnregisteredEvent(quest));

        return removed;
    }

    /**
     * Called by {@link AbstractQuestProgression#update} when a quest reaches a terminal state: archives it
     * for every assigned player, then unregisters the instance.
     *
     * @param questId the id of the quest that just completed
     */
    public AbstractQuestProgression<?> completeQuest(@Nonnull UUID questId) {
        AbstractQuestProgression<?> quest = getQuest(questId);
        if (quest == null) return null;

        unregisterQuest(questId);

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestCompletedEvent.class, questId)
                    .dispatch(new QuestCompletedEvent(quest));

        return quest;
    }

    public AbstractQuestProgression<?> getQuest(UUID questId) {
        return dataStore.get(questId);
    }

    /**
     * @return every quest currently loaded, whatever its scope or type.
     */
    @Nonnull
    public Collection<AbstractQuestProgression<?>> getAllQuests() {
        return dataStore.getAll();
    }

    /**
     * @return the ids of every loaded quest of a concrete type, empty if none.
     */
    @Nonnull
    public Set<UUID> getQuestIdsForType(@Nonnull Class<? extends AbstractQuestProgression<?>> questClass) {
        return dataStore.getForType(questClass);
    }

    /**
     * Pulls every persisted quest into memory. Not needed in normal operation, where quests are
     * loaded on demand through the scope that references them.
     */
    public void loadAllQuests() {
        dataStore.loadAllFromDisk();
    }

    /**
     * Progresses every quest of the visitor's type on the server. Prefer the overload taking a
     * player's own quest ids: a visitor built for one player discards all the others anyway.
     */
    public <Q extends AbstractQuestProgression<Q>> void progress(@Nonnull QuestVisitor<Q> visitor) {
        progress(visitor, dataStore.getForType(visitor.getQuestType()));
    }

    /**
     * Progresses only the given quests, skipping those of another type. The collection may be a
     * live index: completing a quest unregisters it, and the sets involved tolerate that.
     */
    @SuppressWarnings("unchecked")
    public <Q extends AbstractQuestProgression<Q>> void progress(@Nonnull QuestVisitor<Q> visitor, @Nonnull Collection<UUID> questIds) {
        Class<Q> questType = visitor.getQuestType();

        for (UUID id : questIds) {
            AbstractQuestProgression<?> quest = dataStore.get(id);
            if (!questType.isInstance(quest)) continue;

            ((Q) quest).update(visitor);
        }
    }
}
