package com.martelstudios.openquests.core;

import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.event.IEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.SqlDialect;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The core services running for real over an in-memory database and an event bus of their own,
 * installed so the code reaching them statically finds these. Close it after each test.
 */
public final class QuestHarness implements AutoCloseable {

    private static boolean typesRegistered;

    private final QuestStorage storage;
    private final EventBus events = new EventBus(false);
    private final OpenQuestsServices services;
    private final List<Object> heard = new ArrayList<>();

    public QuestHarness() {
        registerTypes();

        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        storage = new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(url, null, null, null, null, "oqtest_", 4, 10, true, SqlDialect.fromUrl(url)));
        storage.start();

        services = new OpenQuestsServices(storage, 5, events);
        services.listen(events);
        services.install();
    }

    /**
     * Shared by every test in the run: a codec refuses a type registered twice.
     */
    private static synchronized void registerTypes() {
        if (typesRegistered) return;

        AbstractQuestProgression.CODEC.register(TestQuestProgression.TYPE, TestQuestProgression.class, TestQuestProgression.CODEC);
        typesRegistered = true;
    }

    @Nonnull
    public OpenQuestsServices services() {
        return services;
    }

    @Nonnull
    public QuestStorage storage() {
        return storage;
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
        TestQuestProgression quest = new TestQuestProgression();
        services.getQuestProgressionService().registerQuest(quest);
        return quest;
    }

    /**
     * Sends the counting visitor to that quest, as an event would.
     */
    public void count(@Nonnull TestQuestProgression quest, int delta, int target) {
        services.getQuestProgressionService().progress(new Counting(delta, target), List.of(quest.getId()));
    }

    @Override
    public void close() {
        events.shutdown();
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
