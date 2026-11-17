package com.martelstudios.openquests.core;

import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.persistence.TestQuestReward;
import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.SqlDialect;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.teams.OpenQuestTeamType;
import com.martelstudios.openquests.core.teams.TeamTypes;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * The core services running for real over an in-memory database and an event bus of their own,
 * installed so the code reaching them statically finds these. Close it after each test.
 */
public final class QuestHarness implements AutoCloseable {

    static {
        // What the database stores is read back through the codecs, test types included
        AbstractQuestProgression.CODEC.register(TestQuestProgression.TYPE, TestQuestProgression.class, TestQuestProgression.CODEC);
        QuestReward.CODEC.register(TestQuestReward.TYPE, TestQuestReward.class, TestQuestReward.CODEC);
        QuestScope.CODEC.register(TeamQuestScope.TYPE, TeamQuestScope.class, TeamQuestScope.CODEC);
    }

    private final QuestStorage storage;
    private final EventBus events = new EventBus(false);
    private final TestClock clock = new TestClock();
    private final ManualScheduler scheduler = new ManualScheduler(clock);
    private final TestPlayers players = new TestPlayers();
    private final List<OpenQuestTeamType> teamTypes = new CopyOnWriteArrayList<>();
    private final OpenQuestsServices services;
    private final List<Object> heard = new ArrayList<>();

    public QuestHarness() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        storage = new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(url, null, null, null, null, "oqtest_", 4, 10, true, SqlDialect.fromUrl(url)));
        storage.start();

        services = new OpenQuestsServices(storage, 5, Duration.ofHours(24), new Platform(events, clock, scheduler, players, players.types(), TeamTypes.of(teamTypes)));
        services.listen(events);
        services.install();
    }

    @Nonnull
    public OpenQuestsServices services() {
        return services;
    }

    @Nonnull
    public QuestStorage storage() {
        return storage;
    }

    @Nonnull
    public TestClock clock() {
        return clock;
    }

    @Nonnull
    public ManualScheduler scheduler() {
        return scheduler;
    }

    @Nonnull
    public TestPlayers players() {
        return players;
    }

    /**
     * @return the team types the packs would declare, empty until a test adds some.
     */
    @Nonnull
    public List<OpenQuestTeamType> teamTypes() {
        return teamTypes;
    }

    /**
     * Hands a new quest reading that asset to the player, the constraints of the asset asked first.
     *
     * @return the quest, or {@code null} if the player was refused
     */
    @Nullable
    public TestQuestProgression assign(@Nonnull OpenQuestAsset asset, @Nonnull UUID playerId) {
        TestQuestProgression quest = new TestQuestProgression().withAsset(asset);
        return services.getQuestProgressionService().assignQuest(quest, playerId) ? quest : null;
    }

    /**
     * @return the quests the player's own record lists.
     */
    @Nonnull
    public Set<UUID> questsOf(@Nonnull UUID playerId) {
        return services.getPlayerQuestService().getQuests(players.of(playerId)).getAllIds();
    }

    /**
     * Keeps every event of that type early in its dispatch, so one a service dispatches while
     * handling it comes after it.
     */
    public <K, E extends IEvent<K>> void hear(@Nonnull Class<E> type) {
        events.<K, E>registerGlobal(EventPriority.FIRST, type, heard::add);
    }

    /**
     * @return the event types heard so far, in the order they were dispatched.
     */
    @Nonnull
    public List<Class<?>> heard() {
        return heard.stream().<Class<?>>map(Object::getClass).toList();
    }

    /**
     * @return a quest registered the way the services hand one out.
     */
    @Nonnull
    public TestQuestProgression register() {
        return register(null);
    }

    /**
     * @return a quest reading that asset, registered the way the services hand one out.
     */
    @Nonnull
    public TestQuestProgression register(@Nullable OpenQuestAsset asset) {
        TestQuestProgression quest = new TestQuestProgression().withAsset(asset);
        services.getQuestProgressionService().registerQuest(quest);
        return quest;
    }

    /**
     * Sends the counting visitor to that quest, as an event would.
     */
    public void count(@Nonnull TestQuestProgression quest, int delta, int target) {
        services.getQuestProgressionService().progress(new Counting(delta, target), List.of(quest.getId()));
    }

    /**
     * Waits for the storage thread to run what was queued on it. A quest spanning servers is
     * written there, and its end announced there, after the change that made it returned.
     */
    public void settle() {
        CountDownLatch done = new CountDownLatch(1);
        services.getQuestSyncService().execute("settling a test", done::countDown);
        try {
            if (!done.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("The storage thread did not settle");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void close() {
        events.shutdown();
        scheduler.shutdownNow();
        storage.close();
    }

    private record Counting(int delta, int target) implements QuestVisitor<TestQuestProgression> {
        @Override
        public Class<TestQuestProgression> getQuestType() {
            return TestQuestProgression.class;
        }

        @Override
        public void progress(TestQuestProgression quest) {
            quest.addAndEndAt(delta, target);
        }
    }
}
