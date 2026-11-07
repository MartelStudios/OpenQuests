package com.martelstudios.openquests.extension.quests.stat;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoseStatProgressionTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void fractionsAddUpToWholePoints() {
        ManaQuest quest = held(10);

        assertEquals(0, quest.accumulate(0.6));
        assertEquals(1, quest.accumulate(0.6));
    }

    @Test
    void aQuestOverCountsNothing() {
        ManaQuest quest = held(2);
        quest.apply(new QuantityQuestProgression.Add(2));

        lose(PLAYER, 5, quest);

        assertEquals(2, quest.getCurrentQuantity());
    }

    @Test
    void anotherPlayersLossCountsNothing() {
        ManaQuest quest = held(10);

        lose(UUID.randomUUID(), 5, quest);

        assertEquals(0, quest.getCurrentQuantity());
    }

    @Test
    void twoQuestsOnOneStatEachCountTheLoss() {
        ManaQuest first = held(10);
        ManaQuest second = held(10);

        lose(PLAYER, 3, first, second);

        assertEquals(3, first.getCurrentQuantity());
        assertEquals(3, second.getCurrentQuantity());
    }

    @Test
    void onlyTheQuestsStatCounts() {
        ManaQuest quest = held(10);

        new LoseStatQuestVisitor(PLAYER, stat -> stat.equals("Health") ? 5 : 0).progress(quest);

        assertEquals(0, quest.getCurrentQuantity());
    }

    private static void lose(UUID player, double mana, ManaQuest... quests) {
        var visitor = new LoseStatQuestVisitor(player, stat -> stat.equals("Mana") ? mana : 0);
        for (ManaQuest quest : quests) visitor.progress(quest);
    }

    private static ManaQuest held(int target) {
        ManaQuest quest = new ManaQuest.Builder().targetQuantity(target).build();
        quest.apply(new AbstractQuestProgression.Join(PLAYER));
        return quest;
    }

    /**
     * Counts mana without an asset store, which unit tests cannot build.
     */
    static final class ManaQuest extends LoseStatQuestProgression {
        static final class Builder extends QuantityQuestProgression.Builder<LoseStatQuestProgression, Builder> {
            Builder() {
                super(new ManaQuest(), "Mana");
            }

            @Override
            public ManaQuest build() {
                return (ManaQuest) super.build();
            }
        }

        @Nonnull
        @Override
        public String getStat() {
            return "Mana";
        }

        @Override
        public boolean isStopOnComplete() {
            return true;
        }
    }
}
