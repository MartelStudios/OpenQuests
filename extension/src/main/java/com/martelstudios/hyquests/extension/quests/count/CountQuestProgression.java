package com.martelstudios.hyquests.extension.quests.count;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;

import javax.annotation.Nullable;

/**
 * Runtime state shared by every quest whose completion is "reach a target count of something".
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class CountQuestProgression<Q extends CountQuestProgression<Q>> extends AbstractQuestProgression<Q> {

    public static final BuilderCodec<CountQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(CountQuestProgression.class, AbstractQuestProgression.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("Count", Codec.INTEGER), (quest, count) -> quest.count = count, quest -> Integer.valueOf(quest.count))
                                                                                     .add()
                                                                                     .append(new KeyedCodec<>("TargetCount", Codec.INTEGER), (quest, targetCount) -> quest.targetCount = targetCount, quest -> quest.targetCount)
                                                                                     .add()
                                                                                     .build();

    protected int count;

    @Nullable
    protected Integer targetCount;

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
     * @return this instance's target if one was set on it, the asset's otherwise.
     */
    public int getTargetCount() {
        return targetCount != null ? targetCount : getAsset().getCount();
    }

    public Q setTargetCount(@Nullable Integer targetCount) {
        this.targetCount = targetCount;
        return self();
    }

    /**
     * @return {@code true} once {@link #count} reaches the target quantity.
     */
    public boolean checkCompletion() {
        return count >= getTargetCount();
    }
}
