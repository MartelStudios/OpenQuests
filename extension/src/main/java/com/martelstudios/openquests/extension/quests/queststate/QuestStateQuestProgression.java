package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.quests.queststate.QuestStateQuestAsset.QuestStateRequirement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Runtime side of a quest-state quest. Both the watched quest and the requirement live here as
 * well as on the asset, so an instance can watch something of its own.
 */
public class QuestStateQuestProgression extends AbstractQuestProgression<QuestStateQuestProgression> {

    public static final BuilderCodec<QuestStateQuestProgression> CODEC = BuilderCodec.builder(QuestStateQuestProgression.class, QuestStateQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("QuestAssetId", Codec.STRING), (quest, assetId) -> quest.questAssetId = assetId, quest -> quest.questAssetId)
                                                                                     .add()
                                                                                     .append(new KeyedCodec<>("QuestStateRequirement", new EnumCodec<>(QuestStateRequirement.class)), (quest, requirement) -> quest.questStateRequirement = requirement, quest -> quest.questStateRequirement)
                                                                                     .add()
                                                                                     .build();

    /**
     * Overrides the asset's watched quest for this instance alone.
     */
    @Nullable
    protected String questAssetId;

    /**
     * Overrides the asset's requirement for this instance alone.
     */
    @Nullable
    protected QuestStateRequirement questStateRequirement;

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
     * @return this instance's requirement if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestStateRequirement getQuestStateRequirement() {
        return questStateRequirement != null ? questStateRequirement : getAsset().getQuestStateRequirement();
    }

    protected QuestStateQuestProgression() {}

    /**
     * Sets up a quest state quest, whose watched quest and requirement can be overridden for that
     * quest alone. Set before anyone holds it, so the index files it once, under what it watches.
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

        /**
         * Overrides the asset's requirement for this quest alone.
         */
        public Builder questStateRequirement(@Nullable QuestStateRequirement questStateRequirement) {
            quest().questStateRequirement = questStateRequirement;
            return this;
        }

        @Override
        protected void validate(@Nonnull QuestStateQuestProgression quest) {
            super.validate(quest);

            String watched = quest.questAssetId;
            if (watched != null && OpenQuestAsset.getAsset(watched) == null) throw new IllegalStateException("No quest asset '" + watched + "' to watch");
        }
    }

    public boolean isNot() {
        return getAsset().isNot();
    }
}
