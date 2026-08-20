package com.martelstudios.hyquests.core.assignment.conditions;

import com.martelstudios.hyquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.hyquests.core.assignment.services.QuestAssignmentService;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Links {@link QuestAssignmentAsset} to {@link QuestAssignmentCondition} to avoid looping on all declared assignments when a condition updates.
 * Filters which assignments are worth re-evaluating when something happens.
 * <p>
 * A resolver watches its own events and reports a satisfied condition to
 * {@link QuestAssignmentService#setAssignmentConditionSatisfied(QuestAssignmentAsset, QuestAssignmentCondition, UUID)};
 * the assignment decides what that means for the whole.
 */
public interface QuestAssignmentConditionResolver<C extends QuestAssignmentCondition<C>> {
    /**
     * Registers a {@link QuestAssignmentAsset} interested in the {@link QuestAssignmentCondition} resolution
     */
    void register(@Nonnull QuestAssignmentAsset asset, @Nonnull C condition);

    /**
     * Evaluates if the {@link QuestAssignmentCondition} for this playerId is satisfied
     * @param questAssignmentCondition the condition to evaluate
     * @param playerId the player's id to evaluate the condition on
     * @return {@code true} if the condition is met
     */
    boolean evaluate(C questAssignmentCondition, @Nonnull UUID playerId);
}
