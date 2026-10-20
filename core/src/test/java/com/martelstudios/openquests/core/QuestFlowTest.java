package com.martelstudios.openquests.core;

import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestRegisteredEvent;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * What the services do with a quest nobody holds, from its registration to its end, as they do
 * it today. A change here changes what server owners see.
 */
class QuestFlowTest {

    private final QuestHarness harness = new QuestHarness();
    private final QuestProgressionService progression = harness.services().getQuestProgressionService();

    @AfterEach
    void close() {
        harness.close();
    }

    @Test
    void aRegisteredQuestRunsAndIsAnnounced() {
        harness.hear(QuestRegisteredEvent.class);

        TestQuestProgression quest = harness.register();

        assertSame(quest, progression.getLiveQuest(quest.getId()));
        assertEquals(QuestState.IN_PROGRESS, quest.getState());
        assertNotNull(quest.getStartedAt());
        assertEquals(List.of(QuestRegisteredEvent.class), harness.heard());
    }

    @Test
    void progressShortOfTheEndIsOnlyAnUpdate() {
        TestQuestProgression quest = harness.register();
        harness.hear(QuestUpdatedEvent.class);
        harness.hear(QuestCompletedEvent.class);

        harness.count(quest, 2, 5);

        assertEquals(2, quest.getCounter());
        assertEquals(QuestState.IN_PROGRESS, quest.getState());
        assertEquals(List.of(QuestUpdatedEvent.class), harness.heard());
    }

    @Test
    void anEndNobodyHoldsIsAnnouncedThenDoneAway() {
        TestQuestProgression quest = harness.register();
        harness.hear(QuestCompletedEvent.class);
        harness.hear(QuestStateChangedEvent.class);
        harness.hear(QuestUpdatedEvent.class);
        harness.hear(QuestUnregisteredEvent.class);

        harness.count(quest, 5, 5);

        assertEquals(QuestState.SUCCESSFUL, quest.getState());
        assertNotNull(quest.getCompletedAt());
        assertEquals(List.of(QuestCompletedEvent.class, QuestUnregisteredEvent.class, QuestStateChangedEvent.class, QuestUpdatedEvent.class), harness.heard(),
            "the end is announced, the quest left with no one to read it is unregistered while it is, then the new state follows");
        assertNull(progression.getQuest(quest.getId()));
    }

    @Test
    void aQuestThatIsOverTakesNoMoreProgress() {
        TestQuestProgression quest = harness.register();
        harness.count(quest, 5, 5);
        harness.hear(QuestUpdatedEvent.class);

        harness.count(quest, 1, 5);

        assertEquals(5, quest.getCounter());
        assertEquals(List.of(), harness.heard());
    }

    @Test
    void anUnregisteredQuestIsAnnouncedAndDeleted() {
        TestQuestProgression quest = harness.register();
        harness.storage().writeProgressions(List.of(quest));
        assertNotNull(harness.storage().loadProgression(quest.getId()));
        harness.hear(QuestUnregisteredEvent.class);

        progression.unregisterQuest(quest);

        assertNull(progression.getQuest(quest.getId()));
        assertNull(harness.storage().loadProgression(quest.getId()));
        assertEquals(List.of(QuestUnregisteredEvent.class), harness.heard());
    }
}
