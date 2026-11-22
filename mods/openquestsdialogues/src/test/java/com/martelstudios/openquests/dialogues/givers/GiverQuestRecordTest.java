package com.martelstudios.openquests.dialogues.givers;

import com.hypixel.hytale.codec.ExtraInfo;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiverQuestRecordTest {

    private static final Giver A = new Giver("Farmer", UUID.randomUUID(), null);
    private static final Giver B = new Giver("Farmer", UUID.randomUUID(), null);
    private static final Instant T1 = Instant.ofEpochMilli(1_000);
    private static final Instant T2 = Instant.ofEpochMilli(2_000);

    @Test
    void outcomesAreKeptPerNpc() {
        GiverQuestRecord record = new GiverQuestRecord();

        record.ended(A, "Q", QuestState.SUCCESSFUL, T1);
        record.ended(B, "Q", QuestState.FAILED, T2);

        assertEquals(1, record.completions("Farmer", A.npc(), "Q").count(QuestState.SUCCESSFUL));
        assertEquals(0, record.completions("Farmer", B.npc(), "Q").count(QuestState.SUCCESSFUL));
        QuestCompletions any = record.completions("Farmer", null, "Q");
        assertEquals(2, any.count(QuestState.SUCCESSFUL) + any.count(QuestState.FAILED));
        assertEquals(T2, any.getLastCompletedAt());
    }

    @Test
    void nothingRecordedIsNone() {
        assertSame(QuestCompletions.NONE, new GiverQuestRecord().completions("Farmer", null, "Q"));
        assertSame(QuestCompletions.NONE, new GiverQuestRecord().completions("Farmer", A.npc(), "Q"));
    }

    @Test
    void latestTimesWin() {
        GiverQuestRecord record = new GiverQuestRecord();

        record.started(A, "Q", T2);
        record.started(A, "Q", T1);

        assertEquals(T2, record.completions("Farmer", A.npc(), "Q").getLastStartedAt());
        assertEquals(0, record.completions("Farmer", A.npc(), "Q").total());
    }

    @Test
    void recordSurvivesACodecRoundTrip() {
        GiverQuestRecord record = new GiverQuestRecord();
        record.started(A, "Q", T1);
        record.ended(A, "Q", QuestState.ABANDONED, T2);

        ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
        GiverQuestRecord read = GiverQuestRecord.CODEC.decode(GiverQuestRecord.CODEC.encode(record, extraInfo), extraInfo);

        QuestCompletions completions = read.completions("Farmer", A.npc(), "Q");
        assertEquals(1, completions.count(QuestState.ABANDONED));
        assertEquals(T1, completions.getLastStartedAt());
        assertEquals(T2, completions.getLastCompletedAt());
    }

    @Test
    void forgettingAnNpcDropsItsOutcomes() {
        GiverQuestRecord record = new GiverQuestRecord();
        record.ended(A, "Q", QuestState.SUCCESSFUL, T1);
        record.ended(B, "Q", QuestState.SUCCESSFUL, T1);

        assertTrue(record.forgetNpc(A.npc()));
        assertFalse(record.forgetNpc(A.npc()));

        assertEquals(0, record.completions("Farmer", A.npc(), "Q").total());
        assertEquals(2, record.completions("Farmer", null, "Q").total());
    }

    @Test
    void forgetNpcsDropsEveryReleasedOne() {
        Giver kept = new Giver("Smith", UUID.randomUUID(), null);
        GiverQuestRecord record = new GiverQuestRecord();
        record.ended(A, "Q", QuestState.SUCCESSFUL, T1);
        record.ended(B, "Q", QuestState.SUCCESSFUL, T1);
        record.ended(kept, "Q", QuestState.SUCCESSFUL, T1);

        assertTrue(record.forgetNpcs(java.util.Set.of(A.npc(), B.npc())::contains));

        assertEquals(2, record.completions("Farmer", null, "Q").total());
        assertEquals(0, record.completions("Farmer", A.npc(), "Q").total());
        assertEquals(1, record.completions("Smith", kept.npc(), "Q").total());
        assertFalse(record.forgetNpcs(java.util.Set.of(A.npc(), B.npc())::contains));
    }
}
