package com.martelstudios.openquests.extension.rewards.random;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;

import javax.annotation.Nonnull;

/**
 * Says the draw is one of what follows, then each reward the way it previews itself, after its
 * chance.
 */
public final class RandomRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return RandomReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var randomReward = (RandomReward) reward;
        double total = randomReward.getTotalWeight();

        QuestPageRows.appendLine(context, Message.translation("openquests.page.reward.random"));
        for (RandomReward.Entry entry : randomReward.getEntries()) {
            QuestPageRows.appendLine(context, Message.translation("openquests.page.reward.random.chance").param("chance", (int) Math.round(entry.getWeight() * 100 / total)));
            QuestPageRows.renderReward(context, entry.getReward());
        }
    }
}
