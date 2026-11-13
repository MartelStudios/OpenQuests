package com.martelstudios.openquests.extension.quests.quantity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.extension.constraints.time.WithinConstraint;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

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
                                                                                        .append(new KeyedCodec<>("ComboStartedAt", Codec.LONG), (quest, millis) -> quest.comboStartedAt = Instant.ofEpochMilli(millis), quest -> quest.comboStartedAt == null ? null : Long.valueOf(quest.comboStartedAt.toEpochMilli()))
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

    /**
     * When the open combo window started, {@code null} with no {@code Within} or before the first action.
     */
    @Nullable
    private Instant comboStartedAt;

    @Override
    public QuantityQuestAsset getAsset() {
        return (QuantityQuestAsset) super.getAsset();
    }

    /**
     * @return how far the quest went, nothing once its combo window closed.
     */
    public int getCurrentQuantity() {
        return getCurrentQuantityAt(now());
    }

    /**
     * @return how far the quest stands at that moment, nothing once its combo window closed by then
     * short of the target.
     */
    public int getCurrentQuantityAt(@Nonnull Instant now) {
        Instant endsAt = getComboEndsAt();
        return endsAt != null && !now.isBefore(endsAt) && !isCompleted() ? 0 : quantity;
    }

    /**
     * @return the combo window of the asset's {@code Within}, {@code null} for a quest counting at any pace.
     */
    @Nullable
    protected Duration getComboWindow() {
        OpenQuestAsset asset = super.getAsset();
        if (asset == null) return null;

        for (QuestConstraint constraint : asset.getConstraints()) {
            if (constraint instanceof WithinConstraint within) return within.getWindow();
        }
        return null;
    }

    /**
     * @return when the open combo window closes, {@code null} when none is open.
     */
    @Nullable
    public Instant getComboEndsAt() {
        Duration window = getComboWindow();
        return window == null || comboStartedAt == null ? null : comboStartedAt.plus(window);
    }

    /**
     * Drops what a missed combo had counted. A type remembering more than the count forgets it here too.
     */
    protected void restartCombo() {
        quantity = 0;
    }

    /**
     * Weighs where the quest stands after its count moved: done once the target is reached, running
     * again below it, which only a quest kept running by {@code StopOnComplete: false} can be.
     */
    protected void settle() {
        new SetState(checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS).applyTo(this);
    }

    /**
     * Adds to the count, over whatever it stands at on the copy it is made on. Carries the moment
     * of the action, so a copy written elsewhere judges a combo window as this one did.
     */
    public record Add(int amount, @Nonnull Instant at) implements QuestOperation<QuantityQuestProgression<?>> {

        /**
         * An action made now.
         */
        public Add(int amount) {
            this(amount, Instant.now());
        }

        @Override
        public boolean applyTo(@Nonnull QuantityQuestProgression<?> quest) {
            if (quest.isOver() || amount <= 0) return false;

            Duration window = quest.getComboWindow();
            if (window != null && (quest.comboStartedAt == null || !at.isBefore(quest.comboStartedAt.plus(window)))) {
                quest.restartCombo();
                quest.comboStartedAt = at;
            }

            quest.quantity += amount;
            quest.settle();
            return true;
        }

        /**
         * Two counts made at the same moment are one count of both. Counts made apart stay apart,
         * since a combo window may close between them.
         */
        @Nullable
        @Override
        public QuestOperation<QuantityQuestProgression<?>> followedBy(@Nonnull QuestOperation<?> next) {
            return next instanceof Add more && more.at.equals(at) ? new Add(amount + more.amount, at) : null;
        }
    }

    /**
     * Moves the count to that value outright and weighs the quest again.
     *
     * @return whether the count or the state moved
     */
    protected boolean moveCountTo(int quantity) {
        if (isOver()) return false;

        QuestState before = getState();
        boolean moved = this.quantity != quantity;
        this.quantity = quantity;
        settle();
        return moved || getState() != before;
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
     * @return {@code true} once the count reaches the target, judged on the count as last written rather than at the moment of reading.
     */
    public boolean checkCompletion() {
        return quantity >= getTargetQuantity();
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
