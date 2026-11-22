package com.martelstudios.openquests.dialogues.givers;

import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiverQuestRecorderTest {

    private static final Giver GIVER = new Giver("Farmer", UUID.randomUUID(), null);

    @Test
    void outcomesFollowTheRecipientsCoreCounted() {
        UUID counted = UUID.randomUUID();
        UUID joinedSince = UUID.randomUUID();
        UUID gaveUp = UUID.randomUUID();
        TestQuest quest = new TestQuest();
        quest.apply(new AbstractQuestProgression.Join(joinedSince));
        quest.apply(new AbstractQuestProgression.Join(gaveUp));
        quest.apply(new AbstractQuestProgression.Abandon(gaveUp));
        QuestCompletedEvent event = new QuestCompletedEvent(quest, QuestState.SUCCESSFUL, 1, true);
        event.recipients(() -> Set.of(counted));

        assertEquals(Map.of(counted, QuestState.SUCCESSFUL, gaveUp, QuestState.ABANDONED), GiverQuestRecorder.outcomes(event));
    }

    @Test
    void aRecordReadableHereIsWrittenAtOnce() {
        GiverQuestRecord here = new GiverQuestRecord();
        boolean[] queued = {false};

        GiverQuestRecorder.write(here, record -> record.ended(GIVER, "Q", QuestState.SUCCESSFUL, Instant.now()), () -> queued[0] = true);

        assertEquals(1, here.completions("Farmer", GIVER.npc(), "Q").count(QuestState.SUCCESSFUL));
        assertFalse(queued[0]);
    }

    @Test
    void aRecordOutOfReachIsQueued() {
        boolean[] queued = {false};

        GiverQuestRecorder.write(null, record -> {}, () -> queued[0] = true);

        assertTrue(queued[0]);
    }
}
