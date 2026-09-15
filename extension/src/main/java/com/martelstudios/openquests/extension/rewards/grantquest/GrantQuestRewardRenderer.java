package com.martelstudios.openquests.extension.rewards.grantquest;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;

import javax.annotation.Nonnull;

/**
 * Names the quests this one opens up, which is how a player reads a chain before walking it. One
 * the player already holds becomes a link to it, since the journal has somewhere to send them.
 */
public final class GrantQuestRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return GrantQuestReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        for (String questAssetId : ((GrantQuestReward) reward).getQuestAssetIds()) {
            QuestAsset asset = QuestAsset.getAsset(questAssetId);
            if (asset == null) continue;

            // Named as what it hands over, not just by its title: a reward list is read for what
            // it pays, and a bare quest name there reads as a requirement rather than a gift.
            Message label = Message.translation("openquests.page.reward.grant")
                                   .param("quest", AbstractQuestProgression.titleOf(asset));

            // The quest this very completion handed over where there is one, so a chain walked
            // twice leads back to the run being read rather than to the latest of its twins
            context.granting(questAssetId, () -> {
                String lineSelector = QuestPageRows.appendLine(context, label);

                QuestPageRows.setNextIcon(context, lineSelector);
            });
        }
    }
}
