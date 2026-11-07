package com.martelstudios.openquests.extension.rewards.droplist;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;

import javax.annotation.Nonnull;

/**
 * A chest in the slot, then the list's name and how many rolls. What the list holds stays hidden:
 * it is a draw, and naming every possible item would read as a promise of all of them.
 */
public final class DroplistRewardRenderer implements QuestRewardRenderer {

    /**
     * Any chest would do; this one reads as loot rather than as furniture.
     */
    private static final String CHEST_ITEM_ID = "Furniture_Ancient_Chest_Small";

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return DroplistReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var droplistReward = (DroplistReward) reward;

        // Drop lists carry no translated name, so the name is read from the id
        Message label = Message.translation("openquests.page.reward.droplist")
                               .param("droplist", droplistReward.getDroplist().replace('_', ' '));

        QuestPageRows.appendItemLine(context, CHEST_ITEM_ID, label, "x" + droplistReward.getRolls());
    }
}
