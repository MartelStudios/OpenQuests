package com.martelstudios.openquests.core.replication;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.persistence.QuestReplica;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.ReplicaPoll;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Keeps what several servers share in step, on a thread of its own so no game thread waits on the
 * storage: every few seconds it writes this server's replicas of the shared quests, reads the
 * others' and merges them in, follows the shared indexes, and takes in the messages left for the
 * players hosted here. It also claims every change of state of the shared quests, and runs
 * whatever else is read off the game threads, such as a world's quests as it starts.
 */
public class QuestReplicationService implements ScopeIndexes.Writer {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final QuestStorage storage;

    @Nonnull
    private final QuestProgressionStore progressionStore;

    private final long intervalSeconds;

    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "OpenQuests storage");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * For each shared quest, the revision last seen of each other server's replica, so a poll
     * only carries what moved since.
     */
    private final Map<UUID, Map<String, Long>> seen = new ConcurrentHashMap<>();

    /**
     * @param intervalSeconds how often other servers' writes are looked for, which bounds how
     * late one server sees what another did
     */
    public QuestReplicationService(@Nonnull QuestStorage storage, @Nonnull QuestProgressionStore progressionStore, long intervalSeconds) {
        this.storage = storage;
        this.progressionStore = progressionStore;
        this.intervalSeconds = Math.max(1, intervalSeconds);

        QuestTransitions.setArbiter(this::claim);
    }

    public static QuestReplicationService get() {
        return OpenQuestsCorePlugin.get().getQuestReplicationService();
    }

    /**
     * Starts following the other servers, if any may share the storage.
     */
    public void start() {
        if (!storage.isShared()) return;

        worker.scheduleWithFixedDelay(this::tick, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        LOGGER.atInfo().log("Following the other servers on the quest storage every %d s as %s", intervalSeconds, storage.getReplicaId());
    }

    /**
     * Lets what is queued finish, so a shutdown writes nothing over a storage still being written.
     */
    public void stop() {
        worker.shutdown();
        try {
            if (!worker.awaitTermination(10, TimeUnit.SECONDS)) LOGGER.atWarning().log("The quest storage thread did not finish in time");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Runs a storage task on the storage thread, logged rather than lost if it throws.
     *
     * @param what what the task does, for the log
     */
    public void execute(@Nonnull String what, @Nonnull Runnable task) {
        try {
            worker.execute(() -> {
                try {
                    task.run();
                } catch (Throwable e) {
                    LOGGER.atWarning().withCause(e).log("Failed to read or write %s", what);
                }
            });
        } catch (RejectedExecutionException e) {
            // The server is stopping: the last save pass writes what is still in memory
            LOGGER.atFine().log("Left %s to the last save pass", what);
        }
    }

    /**
     * Writes what changed in an index as soon as it changes, off the game threads: a stopping
     * server stops its worlds before the last save pass could read their indexes, and loses none
     * of what was written this way. Written in order with the rest, a deletion included, and a
     * quest always before the index listing it.
     */
    @Override
    public void flushIndex(@Nonnull QuestsRecord record, @Nonnull String indexKey) {
        execute("the " + indexKey + " index", () -> record.flush(storage, indexKey, this::store));
    }

    /**
     * Written in order with the changes made to the index before.
     */
    @Override
    public void deleteIndex(@Nonnull String key) {
        execute("the " + key + " index", () -> storage.deleteIndex(key));
    }

    /**
     * Writes the quests an index is about to list, so another server reading the index never
     * finds an id whose quest it cannot read yet, and drops it for good.
     */
    private void store(@Nonnull Set<UUID> questIds) {
        List<AbstractQuestProgression<?>> quests = new ArrayList<>();
        for (UUID questId : questIds) {
            collect(QuestProgressionService.get().getQuest(questId), quests);
        }
        progressionStore.save(quests);
    }

    /**
     * A group goes with its steps, which a server reading the group back loads along with it.
     */
    private static void collect(@Nullable AbstractQuestProgression<?> quest, @Nonnull List<AbstractQuestProgression<?>> into) {
        if (quest == null) return;

        into.add(quest);
        if (quest instanceof AbstractCompositeQuestProgression<?> group) {
            for (AbstractQuestProgression<?> step : group.getChildren()) collect(step, into);
        }
    }

    /**
     * A quest only this server holds needs nobody's agreement: where it stands goes out with its next
     * write. A shared one moves the way whichever server claims the change first moves it; this
     * server, ending it, first takes in who else holds it, so that every one of them is paid.
     */
    @Nullable
    private StoredState claim(@Nonnull AbstractQuestProgression<?> quest) {
        if (!isReplicated(quest)) return null;

        try {
            StoredState stored = storage.claimState(quest);
            if (stored == null && quest.isCompleted()) apply(storage.pollReplicas(Map.of(quest.getId(), seenOf(quest.getId()))));
            return stored;
        } catch (RuntimeException e) {
            // A storage out of reach does not stop the game: this server makes the change, as it would alone
            LOGGER.atWarning().withCause(e).log("Failed to claim a change of state of quest %s, making it here", quest.getId());
            return null;
        }
    }

    private void tick() {
        try {
            List<AbstractQuestProgression<?>> shared = new ArrayList<>();
            for (AbstractQuestProgression<?> quest : QuestProgressionService.get().getAllQuests()) {
                if (isReplicated(quest)) shared.add(quest);
            }

            // What was seen of quests no longer running here is of no use any more
            Set<UUID> running = new HashSet<>();
            for (AbstractQuestProgression<?> quest : shared) running.add(quest.getId());
            seen.keySet().retainAll(running);

            // Written first, so that what this server did reaches the others a tick sooner
            progressionStore.save(shared);
            apply(storage.pollReplicas(seenOf(shared)));

            followIndexes();

            Set<UUID> hosted = QuestPlayerStateService.get().getHostedPlayers();
            if (!hosted.isEmpty()) QuestPlayerStateService.get().deliverMessages(storage.loadMessages(hosted));
        } catch (Throwable e) {
            // Anything thrown out of a scheduled task cancels every tick after it, silently
            LOGGER.atWarning().withCause(e).log("Following the other servers on the quest storage failed");
        }
    }

    /**
     * Merges in what other servers wrote, telling the players who see it, and moves the quests
     * running here to where the storage says they stand, when that is later news.
     */
    private void apply(@Nonnull ReplicaPoll poll) {
        for (QuestReplica replica : poll.changed()) {
            seen.computeIfAbsent(replica.questId(), id -> new ConcurrentHashMap<>()).merge(replica.serverId(), replica.revision(), Math::max);

            AbstractQuestProgression<?> quest = QuestProgressionService.get().getQuest(replica.questId());
            if (quest == null || !quest.merge(replica.quest())) continue;

            HytaleServer.get()
                        .getEventBus()
                        .dispatchFor(QuestUpdatedEvent.class, quest.getId())
                        .dispatch(new QuestUpdatedEvent(quest));
        }

        poll.states().forEach((questId, stored) -> {
            AbstractQuestProgression<?> quest = QuestProgressionService.get().getLiveQuest(questId);
            if (quest != null) quest.adoptStoredState(stored);
        });
    }

    /**
     * The universe and the groups this server follows, read in one go.
     */
    private void followIndexes() {
        Set<String> keys = new HashSet<>(ScopeIndexes.get().getLoadedKeys(WorldGroupIndex.GROUP_INDEX_PREFIX));
        keys.add(UniverseQuestService.UNIVERSE_INDEX_KEY);

        Map<String, Set<UUID>> added = ScopeIndexes.get().refresh(keys);

        UniverseQuestService.get().joinAdded(added.getOrDefault(UniverseQuestService.UNIVERSE_INDEX_KEY, Set.of()));
        WorldGroupIndex.get().spreadAdded(added);
    }

    @Nonnull
    private Map<String, Long> seenOf(@Nonnull UUID questId) {
        return Map.copyOf(seen.getOrDefault(questId, Map.of()));
    }

    @Nonnull
    private Map<UUID, Map<String, Long>> seenOf(@Nonnull Collection<AbstractQuestProgression<?>> quests) {
        Map<UUID, Map<String, Long>> known = new HashMap<>();
        for (AbstractQuestProgression<?> quest : quests) {
            known.put(quest.getId(), seenOf(quest.getId()));
        }
        return known;
    }

    /**
     * @return whether other servers may hold that quest at once, which is what makes it merged and
     * its end claimed. A step is held wherever its group is.
     */
    private boolean isReplicated(@Nonnull AbstractQuestProgression<?> quest) {
        QuestScope scope = quest.getHead().getScope();
        return storage.isShared() && scope != null && scope.isReplicated();
    }
}
