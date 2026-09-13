package com.martelstudios.openquests.extension.quests.consumeitem;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

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
        if (quest.isCompleted() && quest.isStopOnComplete()) return;

        if (!quest.getItemToConsume().isBlockTypeIncluded(itemId)) return;

        quest.setCurrentQuantity(quest.getCurrentQuantity() + quantity)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
    }

    @Override
    public Class<ConsumeItemQuestProgression> getQuestType() {
        return ConsumeItemQuestProgression.class;
    }
}
