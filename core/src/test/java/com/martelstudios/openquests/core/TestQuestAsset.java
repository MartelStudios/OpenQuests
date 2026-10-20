package com.martelstudios.openquests.core;

import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * An asset handed to a test quest directly, there being no asset store behind a test.
 */
public final class TestQuestAsset extends OpenQuestAsset {

    public TestQuestAsset(@Nonnull QuestConstraint... constraints) {
        this.id = "Test";
        this.constraints = constraints;
    }

    /**
     * Pays these on success.
     */
    @Nonnull
    public TestQuestAsset paying(@Nonnull QuestReward... rewards) {
        this.successfulRewards = rewards;
        return this;
    }

    /**
     * Pays these to a player giving the quest up.
     */
    @Nonnull
    public TestQuestAsset payingOnAbandon(@Nonnull QuestReward... rewards) {
        this.abandonedRewards = rewards;
        return this;
    }

    @Override
    public AbstractQuestProgression.Builder<?, ?> builder() {
        return AbstractQuestProgression.Builder.of(new TestQuestProgression(), id);
    }
}
