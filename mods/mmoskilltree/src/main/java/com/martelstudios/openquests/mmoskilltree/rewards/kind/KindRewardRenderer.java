package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;
import com.ziggfreed.common.loot.reward.RewardChips;

import javax.annotation.Nonnull;

/**
 * The line CommonLib draws for the same row in MMO Skill Tree's own pages, icon included.
 */
public final class KindRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return KindReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var kindReward = (KindReward) reward;
        var chip = RewardChips.chipFor(kindReward.toSpec());

        if (chip == null) QuestPageRows.appendLine(context, Message.raw(kindReward.getKind()));
        else QuestPageRows.appendItemLine(context, chip.iconItemId(), chip.label(), null);
    }
}
