package com.martelstudios.openquests.mmoskilltree.rewards.reputation;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;
import com.ziggfreed.common.reputation.ReputationRuntime;
import com.ziggfreed.common.reputation.ReputationText;

import javax.annotation.Nonnull;

/**
 * One line with the reputation's icon, worded as CommonLib words its own reputation rewards.
 */
public final class ReputationRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return ReputationReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var reputationReward = (ReputationReward) reward;
        int amount = reputationReward.getAmount();

        var def = ReputationRuntime.service().known(reputationReward.getReputation());
        Message name = def == null ? Message.raw(reputationReward.getReputation()) : ReputationText.name(def);
        Message label = ReputationText.line(amount > 0 ? "reward.gain" : "reward.loss", amount, name);

        QuestPageRows.appendItemLine(context, def == null ? null : def.icon(), label, null);
    }
}
