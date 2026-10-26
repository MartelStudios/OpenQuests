package com.martelstudios.openquests.extension.rewards.item;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;
import com.martelstudios.openquests.extension.journal.QuestRewardRenderer;

import javax.annotation.Nonnull;

/**
 * The item itself, then its name as the game names it, then how many of it.
 */
public final class ItemRewardRenderer implements QuestRewardRenderer {

    @Nonnull
    @Override
    public Class<?> getRewardType() {
        return ItemReward.class;
    }

    @Override
    public void renderPreview(@Nonnull QuestPageContext context, @Nonnull QuestReward reward) {
        var itemReward = (ItemReward) reward;
        Item asset = Item.getAssetMap().getAsset(itemReward.getItemId());

        // An id the server cannot resolve is one the client will not either, so the slot is left
        // empty and the raw id at least says which item was meant
        String itemId = asset == null ? null : itemReward.getItemId();
        Message label = asset == null ? Message.raw(itemReward.getItemId()) : asset.getTranslationMessage();

        QuestPageRows.appendItemLine(context, itemId, label, "x" + itemReward.getQuantity());
    }
}
