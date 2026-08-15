package com.martelstudios.hyquests.core.assignment.assets;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.hyquests.core.assets.QuestAsset;
import com.martelstudios.hyquests.core.assignment.conditions.QuestAssignmentCondition;

import javax.annotation.Nonnull;

/**
 * Binds quests to the conditions a player must meet before they are handed out. Kept apart from
 * {@link QuestAsset} so the same quest can be reached through different prerequisites.
 */
public class QuestAssignmentAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, QuestAssignmentAsset>> {

    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(QuestAssignmentAsset::getAssetStore));

    public static final AssetBuilderCodec<String, QuestAssignmentAsset> CODEC = AssetBuilderCodec.builder(QuestAssignmentAsset.class, QuestAssignmentAsset::new, Codec.STRING, (asset, id) -> asset.id = id, asset -> asset.id, (asset, data) -> asset.data = data, asset -> asset.data)
                                                                                                 .append(new KeyedCodec<>("Conditions", new ArrayCodec<>(QuestAssignmentCondition.CODEC, QuestAssignmentCondition<?>[]::new)), (asset, conditions) -> asset.conditions = conditions, asset -> asset.conditions)
                                                                                                 .add()
                                                                                                 .append(new KeyedCodec<>("QuestAssetIds", Codec.STRING_ARRAY, true), (asset, ids) -> asset.questAssetIds = ids, asset -> asset.questAssetIds)
                                                                                                 .addValidator(Validators.nonEmptyArray())
                                                                                                 .addValidator(Validators.uniqueInArray())
                                                                                                 .addValidatorLate(() -> QuestAsset.VALIDATOR_CACHE.getArrayValidator()
                                                                                                                                                   .late())
                                                                                                 .add()
                                                                                                 .append(new KeyedCodec<>("AutoAssign", Codec.BOOLEAN), (asset, value) -> asset.autoAssign = value, asset -> Boolean.valueOf(asset.autoAssign))
                                                                                                 .add()
                                                                                                 .build();

    private static final QuestAssignmentCondition<?>[] NO_CONDITIONS = new QuestAssignmentCondition[0];

    protected String id;
    protected AssetExtraInfo.Data data;
    protected String[] questAssetIds;
    protected QuestAssignmentCondition<?>[] conditions = NO_CONDITIONS;
    protected boolean autoAssign;

    private QuestAssignmentAsset() {}

    public static AssetStore<String, QuestAssignmentAsset, DefaultAssetMap<String, QuestAssignmentAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(QuestAssignmentAsset.class);
    }

    public static DefaultAssetMap<String, QuestAssignmentAsset> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public static QuestAssignmentAsset getAsset(String assetId) {
        return getAssetMap().getAsset(assetId);
    }

    @Override
    public String getId() {
        return id;
    }

    @Nonnull
    public String[] getQuestAssetIds() {
        return questAssetIds;
    }

    @Nonnull
    public QuestAssignmentCondition<?>[] getConditions() {
        return conditions;
    }

    /**
     * @return {@code true} if every player should be offered this assignment on connection.
     */
    public boolean isAutoAssign() {
        return autoAssign;
    }
}
