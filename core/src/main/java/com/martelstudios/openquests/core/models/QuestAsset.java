package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.codec.AssetCodecMapCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Defines the generic quest template configuration. Extend it to create new quest types or to add new serialized data.
 * Do not use it to declare runtime data. Look at {@link AbstractQuestProgression} for runtime data declaration.
 */
public abstract class QuestAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, QuestAsset>> {
    public static final ValidatorCache<String> VALIDATOR_CACHE = new ValidatorCache<>(new AssetKeyValidator<>(QuestAsset::getAssetStore));

    public static final AssetCodecMapCodec<String, QuestAsset> CODEC = new AssetCodecMapCodec<>(Codec.STRING, (asset, value) -> asset.id = value, asset -> asset.id, (asset, value) -> asset.data = value, asset -> asset.data);

    /**
     * Serializes the fields shared by every quest asset; concrete codecs chain from this.
     */
    public static final BuilderCodec<QuestAsset> BASE_CODEC = BuilderCodec.abstractBuilder(QuestAsset.class)
                                                                          .append(new KeyedCodec<>("TitleKey", Codec.STRING), (asset, key) -> asset.titleKey = key, asset -> asset.titleKey)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("DescriptionKey", Codec.STRING), (asset, key) -> asset.descriptionKey = key, asset -> asset.descriptionKey)
                                                                          .add()
                                                                          .append(new KeyedCodec<>("AutoClaim", Codec.BOOLEAN), (asset, value) -> asset.autoClaim = value, asset -> Boolean.valueOf(asset.autoClaim))
                                                                          .add()
                                                                          .append(new KeyedCodec<>("StartOnConnection", Codec.BOOLEAN), (asset, value) -> asset.startOnConnection = value, asset -> Boolean.valueOf(asset.startOnConnection))
                                                                          .add()
                                                                          .append(new KeyedCodec<>("StopOnComplete", Codec.BOOLEAN), (asset, value) -> asset.stopOnComplete = value, asset -> Boolean.valueOf(asset.stopOnComplete))
                                                                          .add()
                                                                          .append(new KeyedCodec<>("PersistProgression", Codec.BOOLEAN), (asset, value) -> asset.persistProgression = value, asset -> Boolean.valueOf(asset.persistProgression))
                                                                          .add()
                                                                          .append(new KeyedCodec<>("PersistHistory", Codec.BOOLEAN), (asset, value) -> asset.persistHistory = value, asset -> Boolean.valueOf(asset.persistHistory))
                                                                          .add()
                                                                          .append(new KeyedCodec<>("CanBeAbandoned", Codec.BOOLEAN), (asset, value) -> asset.canBeAbandoned = value, asset -> Boolean.valueOf(asset.canBeAbandoned))
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
    protected boolean autoClaim;
    protected boolean startOnConnection;
    protected boolean stopOnComplete = true;
    protected boolean persistProgression = true;
    protected boolean persistHistory = true;
    protected boolean canBeAbandoned = true;
    protected QuestReward[] successfulRewards = NO_REWARDS;
    protected QuestReward[] failedRewards = NO_REWARDS;
    protected QuestReward[] abandonedRewards = NO_REWARDS;

    protected QuestAsset() {}

    @Override
    public String getId() {
        return id;
    }

    /**
     * @return the translation key of the title, or {@code null} when the asset leaves the type to
     * name itself.
     */
    @Nullable
    public String getTitleKey() {
        return titleKey;
    }

    @Nullable
    public String getDescriptionKey() {
        return descriptionKey;
    }

    /**
     * Tags are how an asset says something no field covers, and they carry down from a parent asset.
     *
     * @return {@code true} if this asset declares the tag, whatever values it holds.
     */
    public boolean hasTag(@Nonnull String tag) {
        if (data == null) return false;

        Map<String, String[]> tags = data.getRawTags();
        return tags != null && tags.containsKey(tag);
    }

    public boolean isAutoClaim() {
        return autoClaim;
    }

    /**
     * @return {@code true} to hand this quest to every player on connection, once. A player who
     * already received it is not given it again, whatever became of it.
     */
    public boolean isStartOnConnection() {
        return startOnConnection;
    }

    /**
     * @return {@code false} to keep the quest running once it completed, so its state stays
     * readable and can still change. Every outcome it reaches is paid, which is how a repeatable
     * quest is written; the history keeps the last outcome, filed under the quest own id.
     */
    public boolean isStopOnComplete() {
        return stopOnComplete;
    }

    /**
     * @return {@code false} to keep this quest's progression in memory only, so a restart forgets it.
     */
    public boolean isPersistProgression() {
        return persistProgression;
    }

    /**
     * @return {@code false} to leave no trace once the quest completed. AutoClaim must be enabled to allow rewards.
     * Failed rewards cannot be retried on reconnection, so rewards requiring online players will be lost for offline players.
     */
    public boolean isPersistHistory() {
        return persistHistory;
    }

    /**
     * @return {@code false} to refuse a player giving this quest up. Written on a step of a
     * chain, which abandoned on its own would leave the group asking for something that can
     * no longer be finished — the chain is what the player gives up, not one of its parts.
     */
    public boolean canBeAbandoned() {
        return canBeAbandoned;
    }

    /**
     * @return the rewards matching a terminal state, empty for a quest still in progress.
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

    /**
     * Factory to instantiate the associated QuestProgression
     */
    public abstract AbstractQuestProgression<?> create();

    public static AssetStore<String, QuestAsset, DefaultAssetMap<String, QuestAsset>> getAssetStore() {
        return AssetRegistry.getAssetStore(QuestAsset.class);
    }

    public static DefaultAssetMap<String, QuestAsset> getAssetMap() {
        return getAssetStore().getAssetMap();
    }

    public static QuestAsset getAsset(String assetId) {
        return getAssetMap().getAsset(assetId);
    }
}
