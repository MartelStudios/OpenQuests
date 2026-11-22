package com.martelstudios.openquests.dialogues.givers;

import com.martelstudios.opendialogues.conditions.SourceScope;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.models.QuestTally;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GiverTallyTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final DialogueSource SOURCE_A = new DialogueSource(UUID.randomUUID(), "Farmer", null, null);
    private static final DialogueSource SOURCE_B = new DialogueSource(UUID.randomUUID(), "Farmer", null, null);
    private static final DialogueSource MERCHANT = new DialogueSource(UUID.randomUUID(), "Merchant", null, null);

    private static TestQuest runningFrom(DialogueSource source) {
        TestQuest quest = new TestQuest();
        Giver.of(source).writeOn(quest);
        quest.apply(new AbstractQuestProgression.Join(PLAYER));
        return quest;
    }

    @Test
    void runningQuestsCountOnlyForTheirGiver() {
        List<TestQuest> live = List.of(runningFrom(SOURCE_A), runningFrom(SOURCE_B), runningFrom(MERCHANT), new TestQuest());

        assertEquals(1, GiverTally.of(SourceScope.Npc, SOURCE_A, PLAYER, "Q", new GiverQuestRecord(), live).count(QuestState.IN_PROGRESS));
        assertEquals(2, GiverTally.of(SourceScope.Character, SOURCE_A, PLAYER, "Q", new GiverQuestRecord(), live).count(QuestState.IN_PROGRESS));
    }

    @Test
    void endedQuestsComeFromTheRecord() {
        GiverQuestRecord record = new GiverQuestRecord();
        record.ended(Giver.of(SOURCE_A), "Q", QuestState.SUCCESSFUL, Instant.ofEpochMilli(1_000));

        assertEquals(1, GiverTally.of(SourceScope.Npc, SOURCE_A, PLAYER, "Q", record, List.of()).count(QuestState.SUCCESSFUL));
        assertEquals(0, GiverTally.of(SourceScope.Npc, SOURCE_B, PLAYER, "Q", record, List.of()).count(QuestState.SUCCESSFUL));
        assertEquals(1, GiverTally.of(SourceScope.Character, SOURCE_B, PLAYER, "Q", record, List.of()).count(QuestState.SUCCESSFUL));
    }

    @Test
    void noSourceGivesAnEmptyTally() {
        assertSame(QuestTally.NONE, GiverTally.of(SourceScope.Npc, null, PLAYER, "Q", new GiverQuestRecord(), List.of(runningFrom(SOURCE_A))));
        assertSame(QuestTally.NONE, GiverTally.of(SourceScope.Character, null, PLAYER, "Q", new GiverQuestRecord(), List.of(runningFrom(SOURCE_A))));
    }
}
