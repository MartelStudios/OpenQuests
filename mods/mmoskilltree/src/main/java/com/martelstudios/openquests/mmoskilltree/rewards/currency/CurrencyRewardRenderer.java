package com.martelstudios.openquests.mmoskilltree.rewards.currency;

import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;
import com.ziggfreed.common.commerce.fold.CommerceDefaults;
import com.ziggfreed.common.commerce.page.CurrencyText;
import com.ziggfreed.mmoskilltree.pages.render.CostRenderer;

import javax.annotation.Nonnull;

/**
 * One line with the currency's icon and its amount worded as MMO Skill Tree words it ("2 Mastery
 * Points"), the name alone for a currency the server lacks.
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
        var def = CommerceDefaults.currencyEngine().definition(currencyReward.getCurrency());

        QuestPageRows.appendItemLine(context, def == null ? null : CurrencyText.iconOf(def), CostRenderer.currencyAmountMsg(currencyReward.getCurrency(), currencyReward.getAmount()), null);
    }
}
