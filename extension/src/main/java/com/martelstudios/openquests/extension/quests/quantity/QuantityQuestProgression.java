package com.martelstudios.openquests.extension.quests.quantity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nullable;

/**
 * Runtime state shared by every quest whose completion is "reach a target quantity of something".
 * Concrete subtypes only say how {@link #currentQuantity} is updated; the completion check lives
 * here once.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class QuantityQuestProgression<Q extends QuantityQuestProgression<Q>> extends AbstractQuestProgression<Q> {

    public static final BuilderCodec<QuantityQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(QuantityQuestProgression.class, AbstractQuestProgression.BASE_CODEC)
                                                                                        .append(new KeyedCodec<>("CurrentQuantity", Codec.INTEGER), (quest, quantity) -> quest.currentQuantity = quantity, quest -> Integer.valueOf(quest.currentQuantity))
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("TargetQuantity", Codec.INTEGER), (quest, quantity) -> quest.targetQuantity = quantity, quest -> quest.targetQuantity)
                                                                                        .add()
                                                                                        .build();

    protected int currentQuantity;

    /**
     * Overrides the asset's target for this instance alone. Boxed so that "not overridden" is a
     * state of its own, and so the codec leaves it out entirely.
     */
    @Nullable
    protected Integer targetQuantity;

    @Override
    public QuantityQuestAsset getAsset() {
        return (QuantityQuestAsset) super.getAsset();
    }

    public int getCurrentQuantity() {
        return currentQuantity;
    }

    public Q setCurrentQuantity(int currentQuantity) {
        this.currentQuantity = currentQuantity;
        return self();
    }

    /**
     * @return this instance's target if one was set on it, the asset's otherwise.
     */
    public int getTargetQuantity() {
        return targetQuantity != null ? targetQuantity : getAsset().getTargetQuantity();
    }

    public Q setTargetQuantity(@Nullable Integer targetQuantity) {
        this.targetQuantity = targetQuantity;
        return self();
    }

    /**
     * @return {@code true} once {@link #currentQuantity} reaches the target.
     */
    public boolean checkCompletion() {
        return currentQuantity >= getTargetQuantity();
    }
}
