package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
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

    public QuestStateQuestProgression setQuestAssetId(@Nullable String questAssetId) {
        this.questAssetId = questAssetId;
        return this;
    }

    /**
     * @return this instance's requirement if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public QuestStateRequirement getQuestStateRequirement() {
        return questStateRequirement != null ? questStateRequirement : getAsset().getQuestStateRequirement();
    }

    public QuestStateQuestProgression setQuestStateRequirement(@Nullable QuestStateRequirement questStateRequirement) {
        this.questStateRequirement = questStateRequirement;
        return this;
    }

    public boolean isNot() {
        return getAsset().isNot();
    }
}
