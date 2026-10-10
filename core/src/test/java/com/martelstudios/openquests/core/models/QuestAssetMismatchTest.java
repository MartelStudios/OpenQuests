package com.martelstudios.openquests.core.models;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestAssetMismatchTest {

    @BeforeAll
    static void registerTheStepType() {
        AbstractQuestProgression.registerAssetClass(StepQuest.class, GatherAsset.class);
    }

    @Test
    void aQuestWhoseAssetIsGoneIsSetAside() {
        String mismatch = AbstractQuestProgression.findAssetMismatch(StepQuest.class, "GatherMeat", null);

        assertNotNull(mismatch);
        assertTrue(mismatch.contains("GatherMeat"));
    }

    @Test
    void aQuestWhoseAssetChangedTypeIsSetAside() {
        String mismatch = AbstractQuestProgression.findAssetMismatch(StepQuest.class, "GatherMeat", new CompositeAsset());

        assertNotNull(mismatch);
        assertTrue(mismatch.contains("CompositeAsset"));
    }

    @Test
    void aQuestOnTheAssetTypeItReadsRuns() {
        assertNull(AbstractQuestProgression.findAssetMismatch(StepQuest.class, "GatherMeat", new GatherAsset()));
    }

    @Test
    void anAssetExtendingTheOneReadStillFits() {
        assertNull(AbstractQuestProgression.findAssetMismatch(StepQuest.class, "GatherMeat", new SpecialGatherAsset()));
    }

    @Test
    void aQuestBuiltWithoutAnAssetIdIsLeftAlone() {
        assertNull(new StepQuest().findAssetMismatch());
    }

    @Test
    void aTypeWithAFinalFieldItIsWrittenAsIsRefused() {
        assertThrows(IllegalStateException.class, () -> AbstractQuestProgression.registerAssetClass(FrozenQuest.class, GatherAsset.class));
    }

    private static final class StepQuest extends AbstractQuestProgression<StepQuest> {}

    /**
     * Could never take on a stored copy: its list would stay the one it was built with.
     */
    private static final class FrozenQuest extends AbstractQuestProgression<FrozenQuest> {
        private final List<String> kept = new ArrayList<>();
    }

    private static class GatherAsset extends OpenQuestAsset {
        @Override
        public AbstractQuestProgression<?> create() {
            return new StepQuest();
        }
    }

    private static final class SpecialGatherAsset extends GatherAsset {}

    private static final class CompositeAsset extends OpenQuestAsset {
        @Override
        public AbstractQuestProgression<?> create() {
            return new StepQuest();
        }
    }
}
