package com.martelstudios.openquests.extension.quests.queststate;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.assets.QuestAsset;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

/**
 * Succeeds while the player holds, or once held, a quest built from {@code QuestAssetId} in a
 * matching state.
 */
public class QuestStateQuestAsset extends QuestAsset {

    public static final BuilderCodec<QuestStateQuestAsset> CODEC =
        BuilderCodec.builder(QuestStateQuestAsset.class, QuestStateQuestAsset::new, QuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("QuestAssetId", Codec.STRING, true), (asset, assetId) -> asset.questAssetId = assetId, asset -> asset.questAssetId)
                    .addValidator(Validators.nonNull())
                    .addValidatorLate(() -> QuestAsset.VALIDATOR_CACHE.getValidator().late())
                    .add()
                    .append(new KeyedCodec<>("QuestStateRequirement", new EnumCodec<>(QuestStateRequirement.class), true), (asset, requirement) -> asset.questStateRequirement = requirement, asset -> asset.questStateRequirement)
                    .add()
                    .append(new KeyedCodec<>("Not", Codec.BOOLEAN), (asset, not) -> asset.not = not, asset -> Boolean.valueOf(asset.not))
                    .add()
                    .build();

    protected String questAssetId;
    protected QuestStateRequirement questStateRequirement = QuestStateRequirement.STARTED;
    protected boolean not;

    private QuestStateQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new QuestStateQuestProgression().setAssetId(getId());
    }

    public String getQuestAssetId() {
        return questAssetId;
    }

    public QuestStateRequirement getQuestStateRequirement() {
        return questStateRequirement;
    }

    public boolean isNot() {
        return not;
    }

    public enum QuestStateRequirement {
        STARTED, IN_PROGRESS, COMPLETED, SUCCESSFULLY, FAILED, ABANDONED
    }
}
