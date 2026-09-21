package com.martelstudios.openquests.core.services;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestRegisteredEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    public QuestProgressionService(@Nonnull JavaPlugin plugin, QuestProgressionStore questProgressionStore) {
        this.dataStore = questProgressionStore;

        // EventPriority.LAST, to unregister the quest safely
        plugin.getEventRegistry()
              .registerGlobal(EventPriority.LAST, QuestCompletedEvent.class, this::handleQuestCompleted);
    }

    public static QuestProgressionService get() {
        return OpenQuestsCorePlugin.get().getQuestProgressionService();
    }

    /**
     * Registers a concrete quest type's polymorphic serialization tag, for both its asset and
     * its runtime form, under the same id. Mirrors {@code ObjectivePlugin.registerTask}.
     */
    public <Q extends AbstractQuestProgression<Q>, QAsset extends QuestAsset> void registerQuestType(String id, Class<QAsset> questAssetClass, BuilderCodec<QAsset> questAssetCodec, Class<Q> questClass, BuilderCodec<Q> questCodec) {
        QuestAsset.CODEC.register(id, questAssetClass, questAssetCodec);
        AbstractQuestProgression.CODEC.register(id, questClass, questCodec);
    }

    /**
     * Brings a quest back into memory, reading it from disk if that is what it takes. For a caller
     * meaning to act on the quest; one only looking at it wants {@link #getQuest}, which leaves a
     * quest nobody holds any more where it is.
     */
    @Nullable
    public AbstractQuestProgression<?> loadQuest(@Nonnull UUID questId) {
        return dataStore.load(questId);
    }

    /**
     * Drops a quest from memory without doing away with it: a lookup by its id reads it back.
     */
    public AbstractQuestProgression<?> unloadQuest(@Nonnull UUID questId) {
        return dataStore.unload(questId);
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
     * Unregisters a quest and does away with what was written of it. Cleans up every reference
     * left in any player, world or universe index.
     *
     * @param questId the id of the quest to unregister
     */
    public AbstractQuestProgression<?> unregisterQuest(@Nonnull UUID questId) {
        // Read back first: doing away with a quest has to reach the storage and everyone indexing
        // it, and a quest nobody currently holds is exactly the kind that gets deleted
        AbstractQuestProgression<?> quest = dataStore.load(questId);
        if (quest == null) return null;

        return unregisterQuest(quest);
    }

    /**
     * Unregisters a quest and does away with what was written of it. Cleans up every reference
     * left in any player, world or universe index.
     *
     * @param quest the quest to unregister
     */
    public AbstractQuestProgression<?> unregisterQuest(@Nonnull AbstractQuestProgression<?> quest) {
        AbstractQuestProgression<?> removed = dataStore.removeAndDelete(quest.getId());

        if (removed == null) return null;

        quest.onUnregistered();

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestUnregisteredEvent.class, quest.getId())
                    .dispatch(new QuestUnregisteredEvent(quest));

        return removed;
    }

    /**
     * Takes a quest that ended out of the live store and keeps it, unless its asset asked for it
     * to leave no trace — which is the one case where a completion still deletes.
     *
     * <p>Kept rather than projected down to a record: what the quest was made of, how far it got
     * and which of its steps went which way are all still there to be read, and none of it can be
     * rebuilt once thrown away.
     *
     * @return {@code false} for a quest the live store was not holding.
     */
    public boolean archiveQuest(@Nonnull AbstractQuestProgression<?> quest) {
        if (!quest.isPersistHistory()) return unregisterQuest(quest) != null;

        if (!dataStore.archive(quest)) return false;

        quest.onArchived();
        return true;
    }

    private void handleQuestCompleted(QuestCompletedEvent event) {
        if (!event.getQuest().isStopOnComplete()) return;

        archiveQuest(event.getQuest());
    }

    /**
     * @return the quest under that id, running or ended alike, or {@code null} for one that is no
     * longer in memory. An id read off a player's own index answers here for as long as they are
     * online, which is as long as anything drawing it for them runs.
     */
    @Nullable
    public AbstractQuestProgression<?> getQuest(@Nonnull UUID questId) {
        return dataStore.get(questId);
    }

    /**
     * @return the quest under that id only while it is still running, so a caller meaning to move
     * one never lands on a quest that is already over.
     */
    public AbstractQuestProgression<?> getLiveQuest(UUID questId) {
        return dataStore.getLive(questId);
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
     * Pulls every stored quest into memory. Not needed in normal operation, where quests are
     * loaded on demand through the scope that references them, and ruinous against a database
     * several servers write to.
     */
    public void loadAllQuests() {
        dataStore.loadAll();
    }

    /**
     * Progresses every quest of the visitor's type on the server. Prefer the overload taking a
     * player's own quest ids: a visitor built for one player discards all the others anyway.
     */
    public <Q extends AbstractQuestProgression<?>> void progress(@Nonnull QuestVisitor<Q> visitor) {
        progress(visitor, dataStore.getForType(visitor.getQuestType()));
    }

    /**
     * Progresses only the given quests, skipping those of another type and those already over.
     * The collection may be a live index: completing a quest takes it out of that index, and the
     * sets involved tolerate that.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <Q extends AbstractQuestProgression<?>> void progress(@Nonnull QuestVisitor<Q> visitor, @Nonnull Collection<UUID> questIds) {
        Class<Q> questType = visitor.getQuestType();

        for (UUID id : questIds) {
            // The live half only: an id index holds what a player finished too, and nothing that
            // is over is meant to move again
            AbstractQuestProgression<?> quest = dataStore.getLive(id);
            if (!questType.isInstance(quest)) continue;

            // Raw on purpose: isInstance already proved the pairing, and the self-type cannot say it
            ((AbstractQuestProgression) quest).update(visitor);
        }
    }
}
