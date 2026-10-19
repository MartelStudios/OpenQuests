package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;

/**
 * Succeeds while the player's quests built from {@code QuestAssetId} stand as its requirement
 * asks, and goes back to running when they no longer do.
 */
public class QuestStateQuestAsset extends OpenQuestAsset {

    public static final BuilderCodec<QuestStateQuestAsset> CODEC = QuestRequirement.appendFields(BuilderCodec.builder(QuestStateQuestAsset.class, QuestStateQuestAsset::new, OpenQuestAsset.BASE_CODEC),
                                                                                                 asset -> asset.requirement)
                                                                                   .build();

    @Nonnull
    protected final QuestRequirement requirement = new QuestRequirement();

    private QuestStateQuestAsset() {}

    @Override
    public QuestStateQuestProgression.Builder builder() {
        return new QuestStateQuestProgression.Builder(this);
    }

    public String getQuestAssetId() {
        return requirement.getQuestAssetId();
    }

    @Nonnull
    public QuestRequirement getRequirement() {
        return requirement;
    }
}
