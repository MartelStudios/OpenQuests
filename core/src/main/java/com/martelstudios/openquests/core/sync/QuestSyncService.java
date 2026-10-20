package com.martelstudios.openquests.core.sync;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestStateVersion;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Keeps the quests several servers write in step with the storage, on a thread of its own so no
 * game thread waits on it: writes the changes made here over the latest version, takes in what
 * other servers wrote and holds on to the players hosted here. Each tick then runs the tasks
 * following the rest, and the thread runs whatever else is read off the game threads, such as a
 * world's quests as it starts.
 *
 * <p>Every write of a shared quest runs on that one thread, so this server never writes one quest
 * twice at once.
 */
public class QuestSyncService implements ScopeIndexes.Writer, QuestSync {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final QuestStorage storage;

    @Nonnull
    private final QuestProgressionStore progressionStore;

    @Nonnull
    private final QuestProgressionService progression;

    private final long intervalSeconds;

    private final List<Runnable> eachTick = new CopyOnWriteArrayList<>();

    @Nullable
    private volatile Thread workerThread;

    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "OpenQuests storage");
        thread.setDaemon(true);
        workerThread = thread;
        return thread;
    });

    /**
     * @param intervalSeconds how often other servers' writes are looked for, which bounds how
     * late one server sees what another did
     */
    public QuestSyncService(@Nonnull QuestStorage storage, @Nonnull QuestProgressionStore progressionStore, @Nonnull QuestProgressionService progression, long intervalSeconds) {
        this.storage = storage;
        this.progressionStore = progressionStore;
        this.progression = progression;
        this.intervalSeconds = Math.max(1, intervalSeconds);
    }

    /**
     * Adds a task to every tick, after the shared quests were written and read back. For what else
     * other servers write and this one follows, such as the shared indexes and the player messages.
     */
    public void onEachTick(@Nonnull Runnable task) {
        eachTick.add(task);
    }

    /**
     * Starts following the other servers, if any may share the storage.
     */
    public void start() {
        if (!storage.isShared()) return;

        worker.scheduleWithFixedDelay(this::tick, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        LOGGER.atInfo().log("Following the other servers on the quest storage every %d s", intervalSeconds);
    }

    /**
     * Lets what is queued finish, the shared quests the last save pass handed over included.
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
            // The last save pass ran before the thread stopped, and wrote what was in memory
            LOGGER.atFine().log("Left %s as the server stopped", what);
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
            collect(progressionStore.get(questId), quests);
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
     * A quest is shared once it is stored and something other servers reach holds it; a step is
     * held wherever its group is. Until it is first stored, no other server knows of it.
     */
    @Override
    public boolean isShared(@Nonnull AbstractQuestProgression<?> quest) {
        if (!storage.isShared() || quest.getReplica().getVersion() == 0) return false;

        QuestScope scope = quest.getHead().getScope();
        return scope != null && scope.spansServers();
    }

    /**
     * On the storage thread, like every write of a shared quest, so this server never writes one
     * twice at once. Once that thread is stopping, written in place by whatever runs on it or after
     * it, and by nothing else, which would race it.
     */
    @Override
    public void writeSoon(@Nonnull AbstractQuestProgression<?> quest) {
        try {
            worker.execute(() -> commitLogged(quest));
        } catch (RejectedExecutionException e) {
            if (Thread.currentThread() == workerThread || worker.isTerminated()) {
                commitLogged(quest);
            } else {
                LOGGER.atWarning().log("Quest %s was left unwritten as the server stopped", quest.getId());
            }
        }
    }

    /**
     * Writes the changes made here over the latest version, then tells everyone here what changed:
     * what other servers did meanwhile first, then what this write did. A change of state is heard
     * once, by the write that made it: an end this write found already made pays nothing.
     */
    private void commit(@Nonnull AbstractQuestProgression<?> quest) {
        List<? extends QuestOperation<?>> operations = quest.getReplica().getPendingOperations();
        if (operations.isEmpty()) return;

        long read = quest.getReplica().getVersion();
        QuestState storedBefore = quest.getReplica().getState();

        Rebase rebase = new Rebase(operations);
        AbstractQuestProgression<?> written = storage.commitProgression(quest.getId(), rebase);
        if (written == null) {
            gone(quest);
            return;
        }

        quest.getReplica().restack(written, operations.size());

        if (rebase.foundVersion > read) {
            quest.announceUpdate();
            if (rebase.found.state() != storedBefore) quest.announce(storedBefore, rebase.found, false);
        }
        for (Transition transition : rebase.made) {
            quest.announce(transition.from(), transition.to(), true);
        }
    }

    private void tick() {
        try {
            List<AbstractQuestProgression<?>> following = new ArrayList<>();

            for (AbstractQuestProgression<?> quest : progressionStore.getLoaded()) {
                if (!isShared(quest)) continue;

                // Written first, so that what this server did reaches the others a tick sooner
                if (quest.hasChanges()) {
                    commitLogged(quest);
                } else if (progressionStore.getLive(quest.getId()) != null) {
                    following.add(quest);
                }
            }

            follow(following);

            // Held three ticks, so a tick running late never lets a player go to another server
            storage.renewHosting(Duration.ofSeconds(Math.max(30, 3 * intervalSeconds)));

            for (Runnable task : eachTick) task.run();
        } catch (Throwable e) {
            // Anything thrown out of a scheduled task cancels every tick after it, silently
            LOGGER.atWarning().withCause(e).log("Following the other servers on the quest storage failed");
        }
    }

    /**
     * One quest failing to write leaves the others to be written, and itself to the next tick.
     */
    private void commitLogged(@Nonnull AbstractQuestProgression<?> quest) {
        try {
            commit(quest);
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Failed to write quest %s, trying again next time", quest.getId());
        }
    }

    /**
     * Takes in what other servers wrote of the quests running here, reading back only those whose
     * version moved.
     */
    private void follow(@Nonnull List<AbstractQuestProgression<?>> quests) {
        if (quests.isEmpty()) return;

        Map<UUID, Long> versions = storage.loadVersions(quests.stream().map(AbstractQuestProgression::getId).toList());

        List<UUID> moved = new ArrayList<>();
        for (AbstractQuestProgression<?> quest : quests) {
            Long version = versions.get(quest.getId());
            if (version == null) {
                gone(quest);
            } else if (version > quest.getReplica().getVersion()) {
                moved.add(quest.getId());
            }
        }

        for (AbstractQuestProgression<?> stored : storage.loadProgressions(moved)) {
            AbstractQuestProgression<?> quest = progressionStore.get(stored.getId());
            if (quest != null) take(quest, stored);
        }
    }

    /**
     * Takes on what another server wrote, the changes made here since still standing on top, and
     * tells everyone here.
     */
    private void take(@Nonnull AbstractQuestProgression<?> quest, @Nonnull AbstractQuestProgression<?> stored) {
        if (stored.getReplica().getVersion() <= quest.getReplica().getVersion()) return;

        QuestState before = quest.getReplica().getState();
        QuestStateVersion after = stored.getStateVersion();
        quest.getReplica().restack(stored, 0);

        quest.announceUpdate();
        if (after.state() != before) quest.announce(before, after, false);
    }

    /**
     * Another server did away with a quest running here: it goes here too.
     */
    private void gone(@Nonnull AbstractQuestProgression<?> quest) {
        LOGGER.atInfo().log("Quest %s was done away with on another server", quest.getId());
        progression.unregisterQuest(quest);
    }

    /**
     * Makes this server's changes on a stored copy, noting where the copy stood and the changes of
     * state they made: those are this server's to announce.
     */
    private static final class Rebase implements Predicate<AbstractQuestProgression<?>> {

        @Nonnull
        private final List<? extends QuestOperation<?>> operations;

        private final List<Transition> made = new ArrayList<>();

        private long foundVersion;

        @Nonnull
        private QuestStateVersion found = QuestStateVersion.INITIAL;

        private Rebase(@Nonnull List<? extends QuestOperation<?>> operations) {
            this.operations = operations;
        }

        /**
         * Run again on a newer copy whenever another server wrote first, so it starts over each time.
         */
        @Override
        public boolean test(@Nonnull AbstractQuestProgression<?> stored) {
            foundVersion = stored.getReplica().getVersion();
            found = stored.getStateVersion();
            made.clear();

            boolean changed = false;
            for (QuestOperation<?> operation : operations) {
                QuestState before = stored.getState();
                changed |= stored.getReplica().replay(operation);
                if (stored.getState() != before) made.add(new Transition(before, stored.getStateVersion()));
            }
            return changed;
        }
    }

    private record Transition(@Nonnull QuestState from, @Nonnull QuestStateVersion to) {}
}
