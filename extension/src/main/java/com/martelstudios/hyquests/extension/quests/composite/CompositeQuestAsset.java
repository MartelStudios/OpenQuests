package com.martelstudios.hyquests.extension.quests.composite;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.hyquests.core.assets.QuestAsset;

/**
 * Config for a composite quest: the ids of the {@link QuestAsset}s its children are created
 * from, and how their outcomes combine. Mirrors {@link CompositeQuestProgression}, which resolves
 * its own children by id rather than embedding them.
 */
public class CompositeQuestAsset extends QuestAsset {

    public static final BuilderCodec<CompositeQuestAsset> CODEC = BuilderCodec.builder(CompositeQuestAsset.class, CompositeQuestAsset::new, QuestAsset.BASE_CODEC)
                                                                            .append(new KeyedCodec<>("QuestAssetIds", Codec.STRING_ARRAY), (asset, ids) -> asset.assetIds = ids, asset -> asset.assetIds)
                                                                            .addValidator(Validators.nonEmptyArray())
                                                                            .addValidator(Validators.uniqueInArray())
                                                                            .addValidatorLate(() -> QuestAsset.VALIDATOR_CACHE.getArrayValidator().late())
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Operator", new EnumCodec<>(Operator.class)), (asset, operator) -> asset.operator = operator, asset -> asset.operator)
                                                                            .add()
                                                                            .build();

    protected String[] assetIds = new String[0];
    protected Operator operator = Operator.AND;

    private CompositeQuestAsset() {}

    @Override
    public CompositeQuestProgression create() {
        return new CompositeQuestProgression().setAssetId(getId());
    }

    public String[] getAssetIds() {
        return assetIds;
    }

    public Operator getOperator() {
        return operator;
    }

    public enum Operator {
        AND, OR
    }
}
