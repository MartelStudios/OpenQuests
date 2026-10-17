package com.martelstudios.openquests.mmoskilltree.rewards.xp;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;
import com.ziggfreed.mmoskilltree.i18n.Messages;
import com.ziggfreed.mmoskilltree.pages.render.RewardIconResolver;

import javax.annotation.Nonnull;

/**
 * One line with the skill's icon, the way MMO Skill Tree shows its own XP rewards.
 */
public final class XpRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return XpReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var xpReward = (XpReward) reward;
        Message label = Message.translation("openquestsmmoskilltree.reward.xp").param("skill", Messages.skillNameMsg(xpReward.getSkill()));

        QuestPageRows.appendItemLine(context, RewardIconResolver.iconForSkill(xpReward.getSkill()), label, "+" + xpReward.getAmount());
    }
}
