package com.martelstudios.openquests.extension.rewards.choice;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;

import javax.annotation.Nonnull;

/**
 * Says how many picks there are, then each reward the way it previews itself.
 */
public final class ChoiceRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return ChoiceReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var choiceReward = (ChoiceReward) reward;

        QuestPageRows.appendLine(context, Message.translation("openquests.page.reward.choice").param("count", choiceReward.getChoices()));
        for (ChoiceReward.Entry entry : choiceReward.getEntries()) {
            QuestPageRows.renderReward(context, entry.getReward());
        }
    }
}
