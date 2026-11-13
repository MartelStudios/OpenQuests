package com.martelstudios.openquests.extension.quests.reachvalue;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

/**
 * Reach a value of the game read from a {@link ValueSource}, such as a game flag level or a count
 * of memories. {@code TargetQuantity} is the value to reach, 1 by default.
 */
public class ReachValueQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<ReachValueQuestAsset> CODEC =
        BuilderCodec.builder(ReachValueQuestAsset.class, ReachValueQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Source", ValueSource.CODEC, true), (asset, source) -> asset.source = source, asset -> asset.source)
                    .addValidator(Validators.nonNull())
                    .add()
                    .append(new KeyedCodec<>("Relative", Codec.BOOLEAN), (asset, relative) -> asset.relative = relative, asset -> asset.relative)
                    .add()
                    .build();

    private ValueSource source;
    private boolean relative;

    private ReachValueQuestAsset() {
        targetQuantity = 1;
    }

    @Override
    public QuantityQuestProgression.Builder<ReachValueQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new ReachValueQuestProgression(), getId());
    }

    /**
     * @return where the value to reach is read.
     */
    public ValueSource getSource() {
        return source;
    }

    /**
     * @return whether only what the value gains once the quest is given counts, rather than the
     * value itself.
     */
    public boolean isRelative() {
        return relative;
    }
}
