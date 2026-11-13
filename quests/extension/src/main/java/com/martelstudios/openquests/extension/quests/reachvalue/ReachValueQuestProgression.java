package com.martelstudios.openquests.extension.quests.reachvalue;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Counts the watched value, or what it gained since the first read for a relative quest.
 */
public class ReachValueQuestProgression extends QuantityQuestProgression<ReachValueQuestProgression> {

    public static final BuilderCodec<ReachValueQuestProgression> CODEC =
        BuilderCodec.builder(ReachValueQuestProgression.class, ReachValueQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                    .append(new KeyedCodec<>("Baseline", Codec.INTEGER), (quest, baseline) -> quest.baseline = baseline, quest -> quest.baseline)
                    .add()
                    .build();

    /**
     * The value at the first read of a relative quest, persisted so a restart does not move it.
     */
    @Nullable
    private Integer baseline;

    protected ReachValueQuestProgression() {}

    @Override
    public ReachValueQuestAsset getAsset() {
        return (ReachValueQuestAsset) super.getAsset();
    }

    /**
     * @return the value at the first read of a relative quest, {@code null} before it.
     */
    @Nullable
    public Integer getBaseline() {
        return baseline;
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getSource() == null) return super.getDefaultTitle();

        return asset.getSource().title(getTargetQuantity());
    }

    /**
     * Counts a value just read. A relative quest takes its first read as the baseline, which is
     * one tick after it is given at most.
     */
    public record Read(int value, boolean relative) implements QuestOperation<ReachValueQuestProgression> {
        @Override
        public boolean applyTo(@Nonnull ReachValueQuestProgression quest) {
            if (quest.isOver()) return false;

            boolean baselineSet = relative && quest.baseline == null;
            if (baselineSet) quest.baseline = value;

            int counted = relative ? Math.max(0, value - quest.baseline) : value;
            return new Count(counted).applyTo(quest) || baselineSet;
        }
    }
}
