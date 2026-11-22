package com.martelstudios.openquests.core;

import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.rewards.QuestReward;

import com.hypixel.hytale.assetstore.AssetExtraInfo;

import javax.annotation.Nonnull;
import java.util.Map;

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

    /**
     * Declares these tags, as the asset's {@code Tags} would.
     */
    @Nonnull
    public TestQuestAsset tagged(@Nonnull Map<String, String[]> tags) {
        this.data = new AssetExtraInfo.Data(OpenQuestAsset.class, id, null);
        this.data.putTags(tags);
        return this;
    }

    /**
     * Names this title key.
     */
    @Nonnull
    public TestQuestAsset titled(@Nonnull String titleKey) {
        this.titleKey = titleKey;
        return this;
    }

    @Override
    public AbstractQuestProgression.Builder<?, ?> builder() {
        return AbstractQuestProgression.Builder.of(new TestQuestProgression(), id);
    }
}
