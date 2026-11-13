package com.martelstudios.openquests.extension.quests.reachlocation;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

import static com.martelstudios.openquests.extension.constraints.ConstraintFixtures.decode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReachPathComboTest {

    private static final Instant T0 = Instant.parse("2026-11-13T12:00:00Z");

    @Test
    void spotsReachedInsideTheWindowAreNotCountedTwice() {
        ReachPathQuestProgression path = unorderedPath();

        assertTrue(path.apply(new ReachPathQuestProgression.Reach(0, T0)));
        assertFalse(path.apply(new ReachPathQuestProgression.Reach(0, T0.plusSeconds(1))));
        assertEquals(1, path.getCurrentQuantityAt(T0.plusSeconds(1)));
    }

    @Test
    void aMissedComboLetsTheSpotsBeReachedAgain() {
        ReachPathQuestProgression path = unorderedPath();
        path.apply(new ReachPathQuestProgression.Reach(0, T0));
        path.apply(new ReachPathQuestProgression.Reach(1, T0.plusSeconds(1)));

        assertTrue(path.apply(new ReachPathQuestProgression.Reach(0, T0.plusSeconds(20))));
        assertFalse(path.apply(new ReachPathQuestProgression.Reach(0, T0.plusSeconds(21))));
        assertEquals(1, path.getCurrentQuantityAt(T0.plusSeconds(21)));
    }

    @Test
    void theWholePathInsideTheWindowSucceeds() {
        ReachPathQuestProgression path = unorderedPath();
        for (int spot = 0; spot < 3; spot++) path.apply(new ReachPathQuestProgression.Reach(spot, T0.plusSeconds(spot)));

        assertEquals(QuestState.SUCCESSFUL, path.getState());
    }

    private static ReachPathQuestProgression unorderedPath() {
        return QuantityQuestProgression.Builder.of((ReachPathQuestProgression) new ComboPath(), "Path").targetQuantity(3).build();
    }

    /**
     * An unordered path with a ten second combo, its asset standing in for the store.
     */
    private static final class ComboPath extends ReachPathQuestProgression {
        private static final ReachPathQuestAsset ASSET = decode(ReachPathQuestAsset.CODEC, "{\"Ordered\": false, \"TargetQuantity\": 3}");

        @Override
        public ReachPathQuestAsset getAsset() {
            return ASSET;
        }

        @Nullable
        @Override
        protected Duration getComboWindow() {
            return Duration.ofSeconds(10);
        }
    }
}
