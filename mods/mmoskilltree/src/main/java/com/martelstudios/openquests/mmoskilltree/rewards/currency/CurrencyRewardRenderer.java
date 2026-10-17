package com.martelstudios.openquests.mmoskilltree.rewards.currency;

import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;
import com.ziggfreed.mmoskilltree.pages.render.CostRenderer;

import javax.annotation.Nonnull;

/**
 * One line naming the currency as MMO Skill Tree does.
 */
public final class CurrencyRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return CurrencyReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var currencyReward = (CurrencyReward) reward;

        QuestPageRows.appendLine(context, CostRenderer.currencyNameMsg(currencyReward.getCurrency()), "+" + currencyReward.getAmount());
    }
}
