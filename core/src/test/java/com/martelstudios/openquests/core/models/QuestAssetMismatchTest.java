package com.martelstudios.openquests.core.models;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static final class StepQuest extends AbstractQuestProgression<StepQuest> {}

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
