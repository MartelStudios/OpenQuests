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
 * One line with the skill's icon, like the XP reward's.
 */
public final class LevelRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return LevelReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var levelReward = (LevelReward) reward;
        Message label = Message.translation("openquestsmmoskilltree.reward.level")
                               .param("levels", levelReward.getLevels())
                               .param("skill", Messages.skillNameMsg(levelReward.getSkill()));

        QuestPageRows.appendItemLine(context, RewardIconResolver.iconForSkill(levelReward.getSkill()), label, "+" + levelReward.getLevels());
    }
}
