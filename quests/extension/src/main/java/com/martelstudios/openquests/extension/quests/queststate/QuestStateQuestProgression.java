package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Runtime side of a quest-state quest. The watched quest lives here as well as on the asset, so an
 * instance can watch something of its own.
 */
public class QuestStateQuestProgression extends AbstractQuestProgression<QuestStateQuestProgression> {

    public static final BuilderCodec<QuestStateQuestProgression> CODEC = BuilderCodec.builder(QuestStateQuestProgression.class, QuestStateQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("QuestAssetId", Codec.STRING), (quest, assetId) -> quest.questAssetId = assetId, quest -> quest.questAssetId)
                                                                                     .add()
                                                                                     .build();

    /**
     * Overrides the asset's watched quest for this instance alone.
     */
    @Nullable
    private String questAssetId;

    @Override
    public QuestStateQuestAsset getAsset() {
        return (QuestStateQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's watched quest if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public String getQuestAssetId() {
        return questAssetId != null ? questAssetId : getAsset().getQuestAssetId();
    }


    /**
     * @return what is asked of the watched quests.
     */
    @Nonnull
    public QuestRequirement getRequirement() {
        return getAsset().getRequirement();
    }

    protected QuestStateQuestProgression() {}

    /**
     * Sets up a quest state quest, whose watched quest can be overridden for that quest alone. Set before anyone holds it, so the index files it once, under what it watches.
     */
    public static final class Builder extends AbstractQuestProgression.Builder<QuestStateQuestProgression, Builder> {
        Builder(@Nonnull QuestStateQuestAsset asset) {
            super(new QuestStateQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's watched quest for this quest alone.
         */
        public Builder questAssetId(@Nullable String questAssetId) {
            quest().questAssetId = questAssetId;
            return this;
        }

        @Override
        protected void validate(@Nonnull QuestStateQuestProgression quest) {
            super.validate(quest);

            String watched = quest.questAssetId;
            if (watched != null && OpenQuestAsset.getAsset(watched) == null) throw new IllegalStateException("No quest asset '" + watched + "' to watch");
        }
    }
}
