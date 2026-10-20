package com.martelstudios.openquests.core;

import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;

import javax.annotation.Nonnull;

/**
 * An asset handed to a test quest directly, there being no asset store behind a test.
 */
public final class TestQuestAsset extends OpenQuestAsset {

    public TestQuestAsset(@Nonnull QuestConstraint... constraints) {
        this.id = "Test";
        this.constraints = constraints;
    }

    @Override
    public AbstractQuestProgression.Builder<?, ?> builder() {
        return AbstractQuestProgression.Builder.of(new TestQuestProgression(), id);
    }
}
