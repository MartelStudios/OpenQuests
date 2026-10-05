package com.martelstudios.openquests.extension.quests.quantity;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.replication.ReplicatedCounter;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;

import javax.annotation.Nonnull;

import javax.annotation.Nullable;

/**
 * Runtime state shared by every quest whose completion is "reach a target quantity of something".
 * Concrete subtypes only say how the quantity is updated; the completion check lives here once.
 * The quantity is shared: each server sharing the quest moves its own share of it.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class QuantityQuestProgression<Q extends QuantityQuestProgression<Q>> extends AbstractQuestProgression<Q> {

    /**
     * The slot a count written before counts were shared is read into, the same on every server
     * reading it, so it is counted once.
     */
    private static final String LEGACY_SLOT = "";

    public static final BuilderCodec<QuantityQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(QuantityQuestProgression.class, AbstractQuestProgression.BASE_CODEC)
                                                                                        .append(new KeyedCodec<>("Quantity", ReplicatedCounter.CODEC), (quest, counter) -> quest.quantity.merge(counter), quest -> quest.quantity)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("CurrentQuantity", Codec.INTEGER), (quest, quantity) -> quest.quantity.restore(LEGACY_SLOT, quantity), quest -> null)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("TargetQuantity", Codec.INTEGER), (quest, quantity) -> quest.targetQuantity = quantity, quest -> quest.targetQuantity)
                                                                                        .add()
                                                                                        .build();

    /**
     * How far the quest went, every server sharing it moving its own share.
     */
    protected final ReplicatedCounter quantity = new ReplicatedCounter();

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
     * @return how far the quest went, every server's share counted.
     */
    public int getCurrentQuantity() {
        return (int) quantity.get();
    }

    /**
     * Moves this server's share so that the whole comes to that quantity.
     */
    public Q setCurrentQuantity(int currentQuantity) {
        quantity.set(currentQuantity);
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

    @Override
    protected boolean mergeProgress(@Nonnull Q other) {
        return quantity.merge(other.quantity);
    }
}
