package com.martelstudios.openquests.extension.quests.reachvalue;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReachValueReadTest {

    @Test
    void anAbsoluteQuestCountsTheValueItself() {
        ReachValueQuestProgression quest = quest(3);

        quest.apply(new ReachValueQuestProgression.Read(2, false));
        assertEquals(2, quest.getCurrentQuantity());

        quest.apply(new ReachValueQuestProgression.Read(3, false));
        assertEquals(QuestState.SUCCESSFUL, quest.getState());
    }

    @Test
    void aRelativeQuestCountsOnlyWhatTheValueGains() {
        ReachValueQuestProgression quest = quest(5);

        quest.apply(new ReachValueQuestProgression.Read(12, true));
        assertEquals(12, quest.getBaseline());
        assertEquals(0, quest.getCurrentQuantity());

        quest.apply(new ReachValueQuestProgression.Read(16, true));
        assertEquals(4, quest.getCurrentQuantity());

        quest.apply(new ReachValueQuestProgression.Read(17, true));
        assertEquals(QuestState.SUCCESSFUL, quest.getState());
    }

    @Test
    void aValueFallingBelowTheBaselineCountsNothing() {
        ReachValueQuestProgression quest = quest(5);
        quest.apply(new ReachValueQuestProgression.Read(4, true));

        quest.apply(new ReachValueQuestProgression.Read(1, true));

        assertEquals(0, quest.getCurrentQuantity());
        assertEquals(4, quest.getBaseline());
    }

    private static ReachValueQuestProgression quest(int target) {
        return QuantityQuestProgression.Builder.of(new ReachValueQuestProgression(), "Test").targetQuantity(target).build();
    }
}
