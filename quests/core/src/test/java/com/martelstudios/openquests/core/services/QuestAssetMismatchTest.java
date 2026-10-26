package com.martelstudios.openquests.core.services;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

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
        QuestTypeRegistry.registerAssetClass(StepQuest.class, GatherAsset.class);
    }

    @Test
    void aQuestWhoseAssetIsGoneIsSetAside() {
        String mismatch = QuestTypeRegistry.findAssetMismatch(StepQuest.class, "GatherMeat", null);

        assertNotNull(mismatch);
        assertTrue(mismatch.contains("GatherMeat"));
    }

    @Test
    void aQuestWhoseAssetChangedTypeIsSetAside() {
        String mismatch = QuestTypeRegistry.findAssetMismatch(StepQuest.class, "GatherMeat", new CompositeAsset());

        assertNotNull(mismatch);
        assertTrue(mismatch.contains("CompositeAsset"));
    }

    @Test
    void aQuestOnTheAssetTypeItReadsRuns() {
        assertNull(QuestTypeRegistry.findAssetMismatch(StepQuest.class, "GatherMeat", new GatherAsset()));
    }

    @Test
    void anAssetExtendingTheOneReadStillFits() {
        assertNull(QuestTypeRegistry.findAssetMismatch(StepQuest.class, "GatherMeat", new SpecialGatherAsset()));
    }

    @Test
    void aQuestBuiltWithoutAnAssetIdIsLeftAlone() {
        assertNull(QuestTypeRegistry.findAssetMismatch(new StepQuest()));
    }

    @Test
    void aTypeWithAFinalFieldItIsWrittenAsIsRefused() {
        assertThrows(IllegalStateException.class, () -> QuestTypeRegistry.registerAssetClass(FrozenQuest.class, GatherAsset.class));
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
        public AbstractQuestProgression.Builder<?, ?> builder() {
            return AbstractQuestProgression.Builder.of(new StepQuest(), "GatherMeat");
        }
    }

    private static final class SpecialGatherAsset extends GatherAsset {}

    private static final class CompositeAsset extends OpenQuestAsset {
        @Override
        public AbstractQuestProgression.Builder<?, ?> builder() {
            return AbstractQuestProgression.Builder.of(new StepQuest(), "GatherMeat");
        }
    }
}
