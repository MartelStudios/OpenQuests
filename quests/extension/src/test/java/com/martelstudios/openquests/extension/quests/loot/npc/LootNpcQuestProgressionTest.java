package com.martelstudios.openquests.extension.quests.loot.npc;

import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LootNpcQuestProgressionTest {

    private static LootNpcQuestProgression quest(int target) {
        return new LootNpcQuestProgression.Builder("Spears").targetQuantity(target).build();
    }

    @Test
    void aMissCountsPerEntry() {
        LootNpcQuestProgression quest = quest(3);
        quest.apply(new LootNpcQuestProgression.Miss(1));
        quest.apply(new LootNpcQuestProgression.Miss(1));

        assertEquals(2, quest.getMisses(1));
        assertEquals(0, quest.getMisses(0));
    }

    @Test
    void resetClearsOneEntry() {
        LootNpcQuestProgression quest = quest(3);
        quest.apply(new LootNpcQuestProgression.Miss(0));
        quest.apply(new LootNpcQuestProgression.Miss(0));
        quest.apply(new LootNpcQuestProgression.Miss(1));
        quest.apply(new LootNpcQuestProgression.ResetMisses(0));

        assertEquals(0, quest.getMisses(0));
        assertEquals(1, quest.getMisses(1));
    }

    @Test
    void missesSurviveStorage() {
        LootNpcQuestProgression quest = quest(3);
        quest.apply(new LootNpcQuestProgression.Miss(0));
        quest.apply(new LootNpcQuestProgression.Miss(2));

        LootNpcQuestProgression stored = CodecJson.decode(LootNpcQuestProgression.CODEC, CodecJson.encode(LootNpcQuestProgression.CODEC, quest), "quest");

        assertNotNull(stored);
        assertEquals(1, stored.getMisses(0));
        assertEquals(0, stored.getMisses(1));
        assertEquals(1, stored.getMisses(2));
    }

    @Test
    void anOverQuestTakesNoMiss() {
        LootNpcQuestProgression quest = quest(3);
        quest.apply(new QuantityQuestProgression.Add(3));

        assertFalse(quest.apply(new LootNpcQuestProgression.Miss(0)));
        assertFalse(quest.apply(new LootNpcQuestProgression.ResetMisses(0)));
    }

    @Test
    void aNegativeEntryIsRefused() {
        assertFalse(quest(3).apply(new LootNpcQuestProgression.Miss(-1)));
    }

    @Test
    void remainingNeverNegative() {
        LootNpcQuestProgression quest = quest(3);
        assertEquals(3, quest.getRemaining());

        quest.apply(new QuantityQuestProgression.Add(5));
        assertEquals(0, quest.getRemaining());
    }
}
