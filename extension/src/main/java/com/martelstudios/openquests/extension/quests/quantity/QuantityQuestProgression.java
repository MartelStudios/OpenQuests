package com.martelstudios.openquests.extension.quests.quantity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;

/**
 * Runtime state shared by every quest whose completion is "reach a target quantity of something".
 * Concrete subtypes only say how the quantity is updated; the completion check lives here once.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class QuantityQuestProgression<Q extends QuantityQuestProgression<Q>> extends AbstractQuestProgression<Q> {

    public static final BuilderCodec<QuantityQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(QuantityQuestProgression.class, AbstractQuestProgression.BASE_CODEC)
                                                                                        .append(new KeyedCodec<>("CurrentQuantity", Codec.INTEGER), (quest, quantity) -> quest.quantity = quantity, quest -> Integer.valueOf(quest.quantity))
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("TargetQuantity", Codec.INTEGER), (quest, quantity) -> quest.targetQuantity = quantity, quest -> quest.targetQuantity)
                                                                                        .add()
                                                                                        .build();

    /**
     * How far the quest went.
     */
    protected int quantity;

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

    /**
     * @return how far the quest went.
     */
    public int getCurrentQuantity() {
        return quantity;
    }

    /**
     * Weighs where the quest stands after its count moved: done once the target is reached, running
     * again below it, which only a quest kept running by {@code StopOnComplete: false} can be.
     */
    protected void settle() {
        setState(checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS);
    }

    /**
     * Adds to the count, over whatever it stands at on the copy it is made on.
     */
    public record Add(int amount) implements QuestOperation<QuantityQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull QuantityQuestProgression<?> quest) {
            if (quest.isOver() || amount <= 0) return false;

            quest.quantity += amount;
            quest.settle();
            return true;
        }

        /**
         * Two counts in a row are one count of both, steps walked between two writes going as one.
         */
        @Nullable
        @Override
        public QuestOperation<QuantityQuestProgression<?>> followedBy(@Nonnull QuestOperation<?> next) {
            return next instanceof Add more ? new Add(amount + more.amount) : null;
        }
    }

    /**
     * Sets the count outright, for a quest counting what the player holds rather than what they did.
     */
    public record Count(int quantity) implements QuestOperation<QuantityQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull QuantityQuestProgression<?> quest) {
            if (quest.isOver()) return false;

            QuestState before = quest.getState();
            boolean moved = quest.quantity != quantity;
            quest.quantity = quantity;
            quest.settle();
            return moved || quest.getState() != before;
        }
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
     * A default title built from the target and what is being counted.
     *
     * @param counted what the quest counts, named when it is an item or a resource type
     */
    @Nonnull
    protected Message countedTitle(@Nonnull String messageKey, @Nullable QuestItemFilter counted) {
        Message name = counted == null ? null : counted.getName();
        Message item = name != null ? name : Message.translation("openquests.quest.default.something");

        return Message.translation(messageKey)
                      .param("quantity", getTargetQuantity())
                      .param("item", item);
    }

    /**
     * @return {@code true} once the quantity reaches the target.
     */
    public boolean checkCompletion() {
        return getCurrentQuantity() >= getTargetQuantity();
    }

    /**
     * A counted quest can say where it stands, so it answers on the counter rather than falling
     * back to the two answers a binary quest has.
     */
    @Override
    public boolean hasProgressed() {
        return getCurrentQuantity() > 0 || super.hasProgressed();
    }
}
