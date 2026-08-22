package com.martelstudios.openquests.extension.quests.composite;

import com.hypixel.hytale.assetstore.codec.ContainedAssetCodec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.assets.QuestAsset;

/**
 * Config for a composite quest: the {@link QuestAsset}s its children are created from, and how
 * their outcomes combine. Each entry is either the id of an existing asset or an inline definition,
 * which is registered as an asset of its own under a generated id.
 */
public class CompositeQuestAsset extends QuestAsset {

    public static final BuilderCodec<CompositeQuestAsset> CODEC = BuilderCodec.builder(CompositeQuestAsset.class, CompositeQuestAsset::new, QuestAsset.BASE_CODEC)
                                                                            .append(new KeyedCodec<>("QuestAssetIds", new ArrayCodec<>(new ContainedAssetCodec<>(QuestAsset.class, QuestAsset.CODEC), String[]::new)), (asset, ids) -> asset.assetIds = ids, asset -> asset.assetIds)
                                                                            .addValidator(Validators.nonEmptyArray())
                                                                            .addValidator(Validators.uniqueInArray())
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
