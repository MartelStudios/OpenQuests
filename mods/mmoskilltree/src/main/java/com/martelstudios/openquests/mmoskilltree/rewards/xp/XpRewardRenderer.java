package com.martelstudios.openquests.mmoskilltree.rewards.xp;

import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;
import com.ziggfreed.common.loot.reward.RewardChips;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * The line MMO Skill Tree draws for its own XP rewards ("+250 Mining XP"), with the skill's icon.
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
        var chip = RewardChips.chipFor(RewardSpec.of("Mmo_Xp", Map.of("skill", xpReward.getSkill(), "amount", Long.toString(xpReward.getAmount()))));
        if (chip == null) return;

        QuestPageRows.appendItemLine(context, chip.iconItemId(), chip.label(), null);
    }
}
