package com.martelstudios.openquests.extension.conditions.queststate;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.assets.QuestAsset;
import com.martelstudios.openquests.core.assignment.conditions.QuestAssignmentCondition;
import com.martelstudios.openquests.core.assignment.conditions.QuestAssignmentConditionResolver;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Satisfied when the player holds, or once held, a quest built from {@code QuestAssetId} in a
 * matching state. One match is enough, so repeatable quests need no special casing.
 */
public class QuestStateCondition extends QuestAssignmentCondition<QuestStateCondition> {

    public static final BuilderCodec<QuestStateCondition> CODEC = BuilderCodec.builder(QuestStateCondition.class, QuestStateCondition::new, QuestAssignmentCondition.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("QuestAssetId", Codec.STRING, true), (condition, assetId) -> condition.questAssetId = assetId, condition -> condition.questAssetId)
                                                                              .addValidator(Validators.nonNull())
                                                                              .addValidatorLate(() -> QuestAsset.VALIDATOR_CACHE.getValidator()
                                                                                                                                .late())
                                                                              .add()
                                                                              .append(new KeyedCodec<>("QuestStateRequirement", new EnumCodec<>(QuestStateRequirement.class), true), (condition, questStateRequirement) -> condition.questStateRequirement = questStateRequirement, condition -> condition.questStateRequirement)
                                                                              .add()
                                                                              .build();

    protected String questAssetId;
    protected QuestStateRequirement questStateRequirement = QuestStateRequirement.STARTED;

    private QuestStateCondition() {}

    @Override
    public QuestAssignmentConditionResolver<QuestStateCondition> getResolver() {
        return QuestStateConditionFeature.getResolver();
    }

    public String getQuestAssetId() {
        return questAssetId;
    }

    /**
     * Groups {@link QuestState}s the way assets want to talk about them.
     */
    public enum QuestStateRequirement {
        STARTED, IN_PROGRESS, COMPLETED, SUCCESSFULLY, FAILED, ABANDONED
    }
}
