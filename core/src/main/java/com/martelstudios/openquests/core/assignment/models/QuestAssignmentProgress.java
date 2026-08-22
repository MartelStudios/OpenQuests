package com.martelstudios.openquests.core.assignment.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.openquests.core.assignment.conditions.QuestAssignmentCondition;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * What a player has already satisfied on an assignment that is not instantiated, as a bit per
 * condition. Reordering an asset's conditions invalidates the progress cached for
 * it.
 * The main purpose of this, is to avoid instantiating every {@link QuestAssignmentAsset#isAutoAssign()}. It allows to only persists auto assigned assignments currently in progress.
 */
public class QuestAssignmentProgress {
    /**
     * A bitmask caps how many conditions one assignment can have.
     */
    public static final int MAX_CONDITIONS = Long.SIZE;

    public static final BuilderCodec<QuestAssignmentProgress> CODEC = BuilderCodec.builder(QuestAssignmentProgress.class, QuestAssignmentProgress::new)
                                                                                  .append(new KeyedCodec<>("AssignmentAssetId", Codec.STRING), (progress, assetId) -> progress.assignmentAssetId = assetId, progress -> progress.assignmentAssetId)
                                                                                  .add()
                                                                                  .append(new KeyedCodec<>("Satisfied", Codec.LONG), (progress, satisfied) -> progress.completion = satisfied, progress -> Long.valueOf(progress.completion))
                                                                                  .add()
                                                                                  .build();

    protected String assignmentAssetId;
    protected long completion;

    protected QuestAssignmentProgress() {}

    public QuestAssignmentProgress(@Nonnull String assignmentAssetId) {
        this.assignmentAssetId = assignmentAssetId;
    }

    public String getAssignmentAssetId() {
        return assignmentAssetId;
    }

    /**
     * The auto counterpart of {@link QuestAssignment#evaluate}: the asset stands in for the
     * assignment, this holds what is already met.
     *
     * @return {@code true} when nothing is left unsatisfied.
     */
    public boolean evaluate(@Nonnull QuestAssignmentAsset asset, @Nonnull UUID playerId) {
        QuestAssignmentCondition<?>[] conditions = asset.getConditions();
        boolean allSatisfied = true;

        try (var _ = EntityComponents.cache()) {
            for (int i = 0; i < conditions.length; i++) {
                if (isConditionSatisfied(i)) continue;

                QuestAssignmentCondition<?> condition = conditions[i];
                if (!condition.evaluate(playerId)) {
                    allSatisfied = false;
                    continue;
                }

                if (condition.useCache()) setConditionSatisfied(i);
            }
        }

        return allSatisfied;
    }

    /**
     * Latches the condition a resolver just watched become satisfied, so it is not checked again. A
     * condition nested in an operator has no bit of its own and simply falls through, the operator
     * answering for it from its children.
     */
    public void setConditionSatisfied(@Nonnull QuestAssignmentAsset asset, @Nonnull QuestAssignmentCondition<?> condition) {
        QuestAssignmentCondition<?>[] conditions = asset.getConditions();

        for (int i = 0; i < conditions.length; i++) {
            if (conditions[i] != condition) continue;

            if (condition.useCache()) setConditionSatisfied(i);
            break;
        }
    }

    /**
     * @throws IndexOutOfBoundsException past the bitmask width, rather than latching nothing.
     */
    public void setConditionSatisfied(int conditionIndex) {
        if (conditionIndex >= MAX_CONDITIONS) {
            throw new IndexOutOfBoundsException("Condition index " + conditionIndex + " is out of bounds for " + MAX_CONDITIONS + " conditions");
        }

        if (isConditionSatisfied(conditionIndex)) return;

        completion |= 1L << conditionIndex;
    }

    public boolean isConditionSatisfied(int conditionIndex) {
        return conditionIndex < MAX_CONDITIONS && (completion & (1L << conditionIndex)) != 0;
    }

    public boolean isEmpty() {
        return completion == 0;
    }
}
