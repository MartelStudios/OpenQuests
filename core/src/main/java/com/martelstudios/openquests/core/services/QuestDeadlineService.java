package com.martelstudios.openquests.core.services;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestLoadedEvent;
import com.martelstudios.openquests.core.events.QuestRegisteredEvent;
import com.martelstudios.openquests.core.events.QuestUnloadedEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.SetStateVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Ends the quests whose constraints give them a deadline, as each deadline comes. One timer for
 * the whole server, armed on the earliest one, so running quests cost nothing between two expiries
 * however many of them are timed.
 *
 * <p>Only quests in memory are watched. One read back after its deadline passed ends as soon as
 * it is, which is how a player coming back to a quest that ran out while they were away finds it
 * over.
 */
public class QuestDeadlineService {

    /**
     * Left to a player bringing an expired quest back, so its outcome reaches their session rather
     * than racing the connection that is still setting it up.
     */
    private static final Duration ARRIVAL_GRACE = Duration.ofSeconds(5);

    /**
     * How long to wait for a holder still between worlds, whose world thread is where the quest ends.
     */
    private static final Duration HOLDER_RETRY = Duration.ofSeconds(1);

    private final PriorityQueue<Scheduled> queue = new PriorityQueue<>(Comparator.comparing(Scheduled::at));

    /**
     * The one entry each quest has in the queue, so a quest read back twice is not ended twice.
     */
    private final Map<UUID, Scheduled> scheduled = new HashMap<>();

    @Nullable
    private ScheduledFuture<?> timer;

    @Nullable
    private Instant timerAt;

    public QuestDeadlineService(@Nonnull JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(QuestRegisteredEvent.class, event -> watch(event.getQuest(), false));
        plugin.getEventRegistry().registerGlobal(QuestLoadedEvent.class, event -> watch(event.getQuest(), true));
        plugin.getEventRegistry().registerGlobal(QuestUnloadedEvent.class, event -> forget(event.getQuest().getId()));
        plugin.getEventRegistry().registerGlobal(QuestUnregisteredEvent.class, event -> forget(event.getQuest().getId()));
        plugin.getEventRegistry().registerGlobal(QuestCompletedEvent.class, this::handleQuestCompletedEvent);
    }

    /**
     * @return the instance the core plugin set up.
     */
    public static QuestDeadlineService get() {
        return OpenQuestsCorePlugin.get().getQuestDeadlineService();
    }

    /**
     * @return the earliest moment a constraint of its asset ends this quest, {@code null} if none
     * gives it one.
     */
    @Nullable
    public static Instant getDeadline(@Nonnull AbstractQuestProgression<?> quest) {
        Expiry expiry = expiryOf(quest);
        return expiry == null ? null : expiry.at();
    }

    /**
     * Also called for a quest being created, before it knows when it started: it says nothing
     * then, and is watched once registered.
     *
     * @param arriving read back rather than created, so an expired one waits for its holder.
     */
    private void watch(@Nonnull AbstractQuestProgression<?> quest, boolean arriving) {
        if (quest.isCompleted()) return;

        Expiry expiry = expiryOf(quest);
        if (expiry == null) return;

        Instant at = expiry.at();
        if (arriving) {
            Instant earliest = Instant.now().plus(ARRIVAL_GRACE);
            if (at.isBefore(earliest)) at = earliest;
        }

        schedule(quest.getId(), at);
    }

    /**
     * A quest leaving the live store for good stops being watched; one kept running by
     * {@code StopOnComplete: false} still ends on time if it goes back to running.
     */
    private void handleQuestCompletedEvent(@Nonnull QuestCompletedEvent questCompletedEvent) {
        AbstractQuestProgression<?> quest = questCompletedEvent.getQuest();
        if (quest.isStopOnComplete()) forget(quest.getId());
    }

    private synchronized void schedule(@Nonnull UUID questId, @Nonnull Instant at) {
        Scheduled previous = scheduled.put(questId, new Scheduled(questId, at));
        if (previous != null) queue.remove(previous);

        queue.add(scheduled.get(questId));
        arm();
    }

    private synchronized void forget(@Nonnull UUID questId) {
        Scheduled previous = scheduled.remove(questId);
        if (previous != null) queue.remove(previous);
    }

    /**
     * Only ever brings the timer forward. One left armed on a deadline that was forgotten since
     * fires on nothing and re-arms on what remains.
     */
    private synchronized void arm() {
        Scheduled next = queue.peek();
        if (next == null) return;
        if (timer != null && timerAt != null && !next.at().isBefore(timerAt)) return;

        if (timer != null) timer.cancel(false);

        long delay = Math.max(0, Duration.between(Instant.now(), next.at()).toMillis());
        timerAt = next.at();
        timer = HytaleServer.SCHEDULED_EXECUTOR.schedule(this::expireDue, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Runs on the server's shared scheduler, so it only hands each quest over to where it ends.
     */
    private void expireDue() {
        List<UUID> due = new ArrayList<>();

        synchronized (this) {
            timer = null;
            timerAt = null;

            Instant now = Instant.now();
            while (!queue.isEmpty() && !queue.peek().at().isAfter(now)) {
                Scheduled entry = queue.poll();
                scheduled.remove(entry.questId());
                due.add(entry.questId());
            }

            arm();
        }

        for (UUID questId : due) {
            expire(questId);
        }
    }

    /**
     * On the world thread of a holder, where every other change to the quest is made. With nobody
     * online there is no such thread, and no session either for the outcome to race.
     */
    private void expire(@Nonnull UUID questId) {
        AbstractQuestProgression<?> quest = QuestProgressionService.get().getLiveQuest(questId);
        if (quest == null || quest.isCompleted()) return;

        Expiry expiry = expiryOf(quest);
        if (expiry == null) return;

        // Moved since it was scheduled, by an asset reloaded under it
        if (expiry.at().isAfter(Instant.now())) {
            schedule(questId, expiry.at());
            return;
        }

        PlayerRef holder = firstOnlineHolder(quest);
        if (holder == null) {
            end(quest, expiry.state());
            return;
        }

        Ref<EntityStore> reference = holder.getReference();
        if (reference == null) {
            schedule(questId, Instant.now().plus(HOLDER_RETRY));
            return;
        }

        reference.getStore().getExternalData().getWorld().execute(() -> end(quest, expiry.state()));
    }

    private static void end(@Nonnull AbstractQuestProgression<?> quest, @Nonnull QuestState state) {
        if (quest.isCompleted()) return;

        QuestProgressionService.get().progress(new SetStateVisitor(state), List.of(quest.getId()));
    }

    @Nullable
    private static PlayerRef firstOnlineHolder(@Nonnull AbstractQuestProgression<?> quest) {
        for (UUID playerId : quest.getPlayers()) {
            PlayerRef online = Universe.get().getPlayer(playerId);
            if (online != null) return online;
        }
        return null;
    }

    /**
     * The earliest deadline wins, and brings the outcome of the constraint that set it.
     */
    @Nullable
    private static Expiry expiryOf(@Nonnull AbstractQuestProgression<?> quest) {
        OpenQuestAsset asset = quest.getAsset();
        if (asset == null) return null;

        Expiry earliest = null;
        for (QuestConstraint constraint : asset.getConstraints()) {
            Instant at = constraint.getDeadline(quest);
            if (at == null) continue;

            if (earliest == null || at.isBefore(earliest.at())) earliest = new Expiry(at, constraint.getExpiredState());
        }
        return earliest;
    }

    private record Scheduled(@Nonnull UUID questId, @Nonnull Instant at) {}

    private record Expiry(@Nonnull Instant at, @Nonnull QuestState state) {}
}
