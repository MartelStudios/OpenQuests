package com.martelstudios.hyquests.core.assets;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.hyquests.core.models.AbstractQuest;
import com.martelstudios.hyquests.core.models.QuestState;
import com.martelstudios.hyquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * Config template a runtime {@link com.martelstudios.hyquests.core.models.AbstractQuest}
 * looks up by id ({@code questAssetId}) to read its immutable definition (title, description,
 * type-specific parameters, ...). Assets live in their own top-level {@link AssetStore}
 * (JSON files under {@code HyQuests/Quests/}), separate from runtime quest state, so editing
 * an asset does not require touching persisted quest saves.
 * <p>
 * {@link #CODEC} is the polymorphic dispatcher (mirrors {@link com.hypixel.hytale.builtin.hytalegenerator.assets.worldstructures.WorldStructureAsset}):
 * each concrete asset type registers under a {@code "Type"} tag. {@link #BASE_CODEC} serializes
 * the fields common to every quest asset so concrete codecs can chain from it.
 */
public abstract class QuestAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, QuestAsset>> {

    public static final AssetCodecMapCodec<String, QuestAsset> CODEC = new AssetCodecMapCodec<>(Codec.STRING, (asset, value) -> asset.id = value, asset -> asset.id, (asset, value) -> asset.data = value, asset -> asset.data);

    public static final BuilderCodec<QuestAsset> BASE_CODEC = BuilderCodec.abstractBuilder(QuestAsset.class)
                                                                          .append(new KeyedCodec<>("TitleKey", Codec.STRING), (asset, key) -> asset.titleKey = key, asset -> asset.titleKey)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("DescriptionKey", Codec.STRING), (asset, key) -> asset.descriptionKey = key, asset -> asset.descriptionKey)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("AutoStart", Codec.BOOLEAN), (asset, value) -> asset.autoStart = value, asset -> Boolean.valueOf(asset.autoStart))
                                                                          .add()
                                                                          .append(new KeyedCodec<>("AutoClaim", Codec.BOOLEAN), (asset, value) -> asset.autoClaim = value, asset -> Boolean.valueOf(asset.autoClaim))
                                                                          .add()
                                                                          .append(new KeyedCodec<>("SuccessfulRewards", new ArrayCodec<>(QuestReward.CODEC, QuestReward[]::new)), (asset, rewards) -> asset.successfulRewards = rewards, asset -> asset.successfulRewards)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("FailedRewards", new ArrayCodec<>(QuestReward.CODEC, QuestReward[]::new)), (asset, rewards) -> asset.failedRewards = rewards, asset -> asset.failedRewards)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("AbandonedRewards", new ArrayCodec<>(QuestReward.CODEC, QuestReward[]::new)), (asset, rewards) -> asset.abandonedRewards = rewards, asset -> asset.abandonedRewards)
                                                                          .add()
                                                                          .build();

    private static final QuestReward[] NO_REWARDS = new QuestReward[0];

    protected String id;
    protected AssetExtraInfo.Data data;
    protected String titleKey;
    protected String descriptionKey;
    protected boolean autoStart;
    protected boolean autoClaim;
    protected QuestReward[] successfulRewards = NO_REWARDS;
    protected QuestReward[] failedRewards = NO_REWARDS;
    protected QuestReward[] abandonedRewards = NO_REWARDS;

    protected QuestAsset() {}

    @Override
    public String getId() {
        return id;
    }

    public static AssetStore<String, QuestAsset, DefaultAssetMap<String, QuestAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(QuestAsset.class);
    }

    public static QuestAsset getAsset(String assetId) {
        return getAssetMap().getAsset(assetId);
    }

    public static DefaultAssetMap<String, QuestAsset> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public boolean isAutoClaim() {
        return autoClaim;
    }

    /**
     * @return the rewards matching a terminal state, empty for a quest still in progress. Single
     * entry point so callers never have to switch on the outcome themselves.
     */
    @Nonnull
    public QuestReward[] getRewards(@Nonnull QuestState state) {
        return switch (state) {
            case SUCCESSFUL -> successfulRewards;
            case FAILED -> failedRewards;
            case ABANDONED -> abandonedRewards;
            default -> NO_REWARDS;
        };
    }

    /**
     * @return {@code true} if reaching that state grants anything at all.
     */
    public boolean hasRewards(@Nonnull QuestState state) {
        return getRewards(state).length > 0;
    }

    public abstract AbstractQuest<?> create();
}
