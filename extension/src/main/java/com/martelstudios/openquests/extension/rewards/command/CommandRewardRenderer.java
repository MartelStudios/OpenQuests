package com.martelstudios.openquests.extension.rewards.command;

import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;

import javax.annotation.Nonnull;

/**
 * Draws nothing. What a command does is a server matter, and its text would expose the console to
 * a player without telling them anything they can use.
 */
public final class CommandRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return CommandReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {}
}
