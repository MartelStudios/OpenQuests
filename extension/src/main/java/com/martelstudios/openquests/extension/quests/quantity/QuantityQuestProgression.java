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
    private int quantity;

    /**
     * Overrides the asset's target for this instance alone. Boxed so that "not overridden" is a
     * state of its own, and so the codec leaves it out entirely.
     */
    @Nullable
    private Integer targetQuantity;

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
        new SetState(checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS).applyTo(this);
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

    /**
     * Sets up a counted quest, whose target can be overridden for that quest alone.
     */
    public abstract static class Builder<Q extends QuantityQuestProgression<Q>, B extends Builder<Q, B>> extends AbstractQuestProgression.Builder<Q, B> {

        protected Builder(@Nonnull Q quest, @Nonnull String assetId) {
            super(quest, assetId);
        }

        /**
         * Overrides the asset's target for this quest alone.
         */
        public B targetQuantity(@Nullable Integer targetQuantity) {
            ((QuantityQuestProgression<?>) quest()).targetQuantity = targetQuantity;
            return self();
        }

        @Override
        protected void validate(@Nonnull Q quest) {
            super.validate(quest);

            Integer target = ((QuantityQuestProgression<?>) quest).targetQuantity;
            if (target != null && target < 1) throw new IllegalStateException("A target quantity of " + target + " leaves nothing to count");
        }

        /**
         * @return a builder for a counted type with nothing of its own to override.
         */
        @Nonnull
        public static <Q extends QuantityQuestProgression<Q>> Builder<Q, ?> of(@Nonnull Q quest, @Nonnull String assetId) {
            return new PlainBuilder<>(quest, assetId);
        }
    }

    private static final class PlainBuilder<Q extends QuantityQuestProgression<Q>> extends Builder<Q, PlainBuilder<Q>> {
        private PlainBuilder(@Nonnull Q quest, @Nonnull String assetId) {
            super(quest, assetId);
        }
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
