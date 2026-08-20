package com.martelstudios.hyquests.extension.quests.count;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;

/**
 * Runtime state shared by every quest whose completion is "reach a target count of something" —
 * mirrors Hytale's {@code CountObjectiveTask}. Concrete subtypes only need to say how {@link #count}
 * gets updated (an event, a recount, ...); the completion check against the asset's target
 * quantity lives here once.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class CountQuestProgression<Q extends CountQuestProgression<Q>> extends AbstractQuestProgression<Q> {

    public static final BuilderCodec<CountQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(CountQuestProgression.class, AbstractQuestProgression.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("Count", Codec.INTEGER), (quest, count) -> quest.count = count, quest -> Integer.valueOf(quest.count))
                                                                                     .add()
                                                                                     .build();

    protected int count;

    @Override
    public CountQuestAsset getAsset() {
        return (CountQuestAsset) super.getAsset();
    }

    public int getCount() {
        return count;
    }

    public Q setCount(int count) {
        this.count = count;
        return self();
    }


    /**
     * @return {@code true} once {@link #count} reaches the asset's target quantity.
     */
    public boolean checkCompletion() {
        return count >= getAsset().getCount();
    }
}
