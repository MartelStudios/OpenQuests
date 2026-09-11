package com.martelstudios.openquests.extension.journal;

import com.martelstudios.openquests.core.rewards.QuestReward;

import javax.annotation.Nonnull;

/**
 * How one reward type previews itself in the journal, so a player sees what a quest is worth
 * before finishing it. Registered the same way a quest type registers its own.
 */
public interface QuestRewardRenderer {

    /**
     * @return the reward class this renders. Subclasses fall back to it.
     */
    @Nonnull
    Class<?> getRewardType();

    /**
     * Draws the reward, as many lines as it takes.
     */
    void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward);
}
