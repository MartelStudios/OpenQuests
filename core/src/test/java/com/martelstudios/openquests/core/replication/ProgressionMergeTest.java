package com.martelstudios.openquests.core.replication;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionMergeTest {

    private static final UUID ALICE = UUID.randomUUID();
    private static final UUID BOB = UUID.randomUUID();

    @AfterEach
    void standAloneAgain() {
        Replica.setLocalId(Replica.DEFAULT_ID);
    }

    @Test
    void copiesOfOneQuestMergeWhoHoldsIt() {
        TestQuestProgression onA = new TestQuestProgression();
        TestQuestProgression onB = copyOf(onA);

        Replica.setLocalId("a");
        onA.move(ALICE, Membership.Status.JOINED);
        Replica.setLocalId("b");
        onB.move(BOB, Membership.Status.JOINED);

        assertTrue(onA.merge(onB));
        assertEquals(Set.of(ALICE, BOB), onA.getPlayers());
    }

    @Test
    void aCopyThatEndedEndsNothingHere() {
        TestQuestProgression onA = new TestQuestProgression();
        TestQuestProgression onB = copyOf(onA);
        onB.setState(QuestState.SUCCESSFUL);

        onA.merge(onB);

        assertEquals(QuestState.IN_PROGRESS, onA.getState());
    }

    @Test
    void aCopyStandsWhereTheStorageSaysAndOnlyTakesInLaterNews() {
        TestQuestProgression onA = new TestQuestProgression();
        onA.setState(QuestState.FAILED);

        onA.restoreStoredState(new StoredState(QuestState.IN_PROGRESS, null, 3));
        assertEquals(QuestState.IN_PROGRESS, onA.getState());
        assertEquals(3, onA.getStateEpoch());

        // A change no later than what the copy holds is no news, whatever it says
        assertFalse(onA.adoptStoredState(new StoredState(QuestState.SUCCESSFUL, null, 3)));
        assertFalse(onA.adoptStoredState(new StoredState(QuestState.FAILED, null, 2)));
        assertEquals(QuestState.IN_PROGRESS, onA.getState());
    }

    @Test
    void anotherQuestIsNeverMergedIn() {
        TestQuestProgression onA = new TestQuestProgression();
        TestQuestProgression other = new TestQuestProgression();
        other.move(ALICE, Membership.Status.JOINED);

        assertFalse(onA.merge(other));
        assertTrue(onA.getPlayers().isEmpty());
    }

    @Test
    void mergingIsNeverAChangeToSave() {
        TestQuestProgression onA = new TestQuestProgression();
        TestQuestProgression onB = copyOf(onA);
        onB.move(BOB, Membership.Status.JOINED);
        onA.consumeChanges();

        onA.merge(onB);

        assertFalse(onA.hasChanges());
    }

    private static TestQuestProgression copyOf(TestQuestProgression quest) {
        return CodecJson.decode(TestQuestProgression.CODEC, CodecJson.encode(TestQuestProgression.CODEC, quest), "quest");
    }
}
