package com.martelstudios.hyquests.core.assignment.conditions;

import com.martelstudios.hyquests.core.assignment.assets.QuestAssignmentAsset;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Narrows which assignments are worth re-evaluating when something happens, so a catalog of
 * thousands is not walked on every event. Each condition type brings its own: what makes a
 * condition worth re-checking is specific to it and cannot be shared.
 * <p>
 * A resolver watches its own events and reports a satisfied condition to
 * {@code QuestAssignmentService.completeAssignmentCondition}; the assignment decides what that means for the whole.
 */
public interface QuestAssignmentConditionResolver<C extends QuestAssignmentCondition<C>> {
    /**
     * Registers a {@link QuestAssignmentAsset} interested in the {@link QuestAssignmentCondition} resolution
     */
    void register(@Nonnull QuestAssignmentAsset asset, @Nonnull C condition);

    boolean evaluate(C questAssignmentCondition, @Nonnull UUID playerId);
}
