package com.martelstudios.openquests.extension.quests.stat;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

/**
 * Lose a number of points of one stat, any the game or a mod defines. {@code TargetQuantity} is in
 * that stat's points, whose scale differs from one stat to the next.
 */
public class LoseStatQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<LoseStatQuestAsset> CODEC =
        BuilderCodec.builder(LoseStatQuestAsset.class, LoseStatQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Stat", Codec.STRING), (asset, stat) -> asset.stat = stat, asset -> asset.stat)
                    .addValidator(Validators.nonNull())
                    .addValidator(EntityStatType.VALIDATOR_CACHE.getValidator())
                    .add()
                    .build();

    private String stat;

    private LoseStatQuestAsset() {}

    /**
     * @return the id of the stat whose losses count.
     */
    @Nonnull
    public String getStat() {
        return stat;
    }

    @Override
    public QuantityQuestProgression.Builder<LoseStatQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new LoseStatQuestProgression(), getId());
    }
}
