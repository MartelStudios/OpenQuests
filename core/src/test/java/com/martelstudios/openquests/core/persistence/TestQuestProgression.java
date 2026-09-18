package com.martelstudios.openquests.core.persistence;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

/**
 * A quest type standing in for the ones the extension ships, so a backend can be exercised without
 * a server to register anything.
 */
public class TestQuestProgression extends AbstractQuestProgression<TestQuestProgression> {

    public static final String TYPE = "Test";

    public static final BuilderCodec<TestQuestProgression> CODEC =
        BuilderCodec.builder(TestQuestProgression.class, TestQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                    .append(new KeyedCodec<>("Counter", Codec.INTEGER), (quest, counter) -> quest.counter = counter, quest -> Integer.valueOf(quest.counter))
                    .add()
                    .build();

    private int counter;

    public int getCounter() {
        return counter;
    }

    public TestQuestProgression setCounter(int counter) {
        this.counter = counter;
        return this;
    }

    /**
     * The base asks its asset whether a finished quest stops there, and there is no asset store
     * behind a test.
     */
    @Override
    public boolean isStopOnComplete() {
        return true;
    }
}
