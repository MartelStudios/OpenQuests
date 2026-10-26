package com.martelstudios.openquests.extension.quests.consumeitem;

import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Counts what a player just ate or drank against the quests targeting it.
 */
public class ConsumeItemQuestVisitor implements QuestVisitor<ConsumeItemQuestProgression> {

    private final UUID playerId;
    private final String itemId;
    private final int quantity;

    public ConsumeItemQuestVisitor(@Nonnull UUID playerId, @Nonnull String itemId, int quantity) {
        this.playerId = playerId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    @Override
    public void progress(ConsumeItemQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;

        if (!quest.getItemToConsume().matches(itemId)) return;

        quest.apply(new QuantityQuestProgression.Add(quantity));
    }

    @Override
    public Class<ConsumeItemQuestProgression> getQuestType() {
        return ConsumeItemQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
