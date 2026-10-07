package com.martelstudios.openquests.core.sync;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * Keeps the quests several servers write in step with the storage, on a thread of its own so no
 * game thread waits on it: writes the changes made here over the latest version, takes in what
 * other servers wrote, follows the shared indexes, holds on to the players hosted here and takes
 * in the messages left for them. It also runs whatever else is read off the game threads, such as a world's quests
 * as it starts.
 *
 * <p>Every write of a shared quest runs on that one thread, so this server never writes one quest
 * twice at once.
 */
public class QuestSyncService implements ScopeIndexes.Writer, QuestSync.Policy {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * How long a caller about to let go of a quest waits for its write.
     */
    private static final long WRITE_NOW_TIMEOUT_SECONDS = 10;

    @Nonnull
    private final QuestStorage storage;

    @Nonnull
    private final QuestProgressionStore progressionStore;

    private final long intervalSeconds;

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
    public QuestSyncService(@Nonnull QuestStorage storage, @Nonnull QuestProgressionStore progressionStore, long intervalSeconds) {
        this.storage = storage;
        this.progressionStore = progressionStore;
        this.intervalSeconds = Math.max(1, intervalSeconds);

        QuestSync.setPolicy(this);
    }

    public static QuestSyncService get() {
        return OpenQuestsCorePlugin.get().getQuestSyncService();
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
     * A quest is shared once it is stored and something other servers reach holds it; a step is
     * held wherever its group is. Until it is first stored, no other server knows of it.
     */
    @Override
    public boolean isShared(@Nonnull AbstractQuestProgression<?> quest) {
        if (!storage.isShared() || quest.getStoredVersion() == 0) return false;

        QuestScope scope = quest.getHead().getScope();
        return scope != null && scope.spansServers();
    }

    @Override
    public void writeSoon(@Nonnull AbstractQuestProgression<?> quest) {
        execute("quest " + quest.getId(), () -> commit(quest));
    }

    /**
     * On the storage thread like every other write of a shared quest, the caller waiting for it,
     * unless that thread is the caller or is gone.
     */
    @Override
    public void writeNow(@Nonnull AbstractQuestProgression<?> quest) {
        if (Thread.currentThread() == workerThread || worker.isTerminated()) {
            commit(quest);
            return;
        }

        try {
            Future<?> written = worker.submit(() -> commit(quest));
            written.get(WRITE_NOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            // Stopping, and the thread is not done yet: the write waits for it rather than racing it
            LOGGER.atWarning().log("Quest %s was left unwritten as the server stopped", quest.getId());
        } catch (ExecutionException e) {
            LOGGER.atWarning().withCause(e.getCause()).log("Failed to write quest %s", quest.getId());
        } catch (TimeoutException e) {
            LOGGER.atWarning().log("Quest %s is still being written", quest.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Writes the changes made here over the latest version, then tells everyone here what changed:
     * what other servers did meanwhile first, then what this write did. A change of state is heard
     * once, by the write that made it: an end this write found already made pays nothing.
     */
    private void commit(@Nonnull AbstractQuestProgression<?> quest) {
        List<? extends AbstractQuestProgression.Change<?>> changes = quest.getPendingChanges();
        if (changes.isEmpty()) return;

        long read = quest.getStoredVersion();
        QuestState storedBefore = quest.getStoredState();

        Rebase rebase = new Rebase(changes);
        AbstractQuestProgression<?> written = storage.commitProgression(quest.getId(), rebase);
        if (written == null) {
            gone(quest);
            return;
        }

        quest.restack(written, changes.size());

        if (rebase.foundVersion > read) {
            quest.announceUpdate();
            if (rebase.foundState != storedBefore) quest.announce(storedBefore, rebase.foundState, rebase.foundTransitions, false);
        }
        for (Transition transition : rebase.made) {
            quest.announce(transition.from(), transition.to(), transition.count(), true);
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
            followIndexes();

            // Held three ticks, so a tick running late never lets a player go to another server
            storage.renewHosting(Duration.ofSeconds(Math.max(30, 3 * intervalSeconds)));

            Set<UUID> hosted = QuestPlayerStateService.get().getHostedPlayers();
            if (!hosted.isEmpty()) QuestPlayerStateService.get().deliverMessages(storage.loadMessages(hosted));
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
            } else if (version > quest.getStoredVersion()) {
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
        if (stored.getStoredVersion() <= quest.getStoredVersion()) return;

        QuestState before = quest.getStoredState();
        QuestState after = stored.getState();
        int transitions = stored.getTransitions();
        quest.restack(stored, 0);

        quest.announceUpdate();
        if (after != before) quest.announce(before, after, transitions, false);
    }

    /**
     * Another server did away with a quest running here: it goes here too.
     */
    private void gone(@Nonnull AbstractQuestProgression<?> quest) {
        LOGGER.atInfo().log("Quest %s was done away with on another server", quest.getId());
        QuestProgressionService.get().unregisterQuest(quest);
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

    /**
     * Makes this server's changes on a stored copy, noting where the copy stood and the changes of
     * state they made: those are this server's to announce.
     */
    private static final class Rebase implements Predicate<AbstractQuestProgression<?>> {

        @Nonnull
        private final List<? extends AbstractQuestProgression.Change<?>> changes;

        private final List<Transition> made = new ArrayList<>();

        private long foundVersion;

        private QuestState foundState = QuestState.IN_PROGRESS;

        private int foundTransitions;

        private Rebase(@Nonnull List<? extends AbstractQuestProgression.Change<?>> changes) {
            this.changes = changes;
        }

        /**
         * Run again on a newer copy whenever another server wrote first, so it starts over each time.
         */
        @Override
        public boolean test(@Nonnull AbstractQuestProgression<?> stored) {
            foundVersion = stored.getStoredVersion();
            foundState = stored.getState();
            foundTransitions = stored.getTransitions();
            made.clear();

            boolean changed = false;
            for (AbstractQuestProgression.Change<?> change : changes) {
                QuestState before = stored.getState();
                changed |= stored.replay(change);
                if (stored.getState() != before) made.add(new Transition(before, stored.getState(), stored.getTransitions()));
            }
            return changed;
        }
    }

    /**
     * @param count how many changes of state the quest went through, this one included
     */
    private record Transition(@Nonnull QuestState from, @Nonnull QuestState to, int count) {}
}
