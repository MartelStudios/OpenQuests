package com.martelstudios.hyquests.extension.quests.craft;

import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.martelstudios.hyquests.core.models.QuestState;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Accumulates what a craft produced, counting one recipe run per unit crafted.
 */
public class CraftQuestVisitor implements QuestVisitor<CraftQuestProgression> {

    private final UUID playerId;
    private final CraftRecipeEvent.Post event;

    public CraftQuestVisitor(@Nonnull UUID playerId, @Nonnull CraftRecipeEvent.Post event) {
        this.playerId = playerId;
        this.event = event;
    }

    @Override
    public void progress(CraftQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted()) return;

        int crafted = countMatchingOutputs(quest);
        if (crafted == 0) return;

        quest.setCurrentQuantity(quest.getCurrentQuantity() + crafted)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
    }

    /**
     * A recipe can yield several matching outputs, and is run {@code quantity} times.
     */
    private int countMatchingOutputs(@Nonnull CraftQuestProgression quest) {
        MaterialQuantity[] outputs = event.getCraftedRecipe().getOutputs();
        if (outputs == null) return 0;

        int matched = 0;
        for (MaterialQuantity output : outputs) {
            if (output.getItemId() == null) continue;
            if (!quest.getItemToCraft().isBlockTypeIncluded(output.getItemId())) continue;

            matched += output.getQuantity();
        }

        return matched * event.getQuantity();
    }

    @Override
    public Class<CraftQuestProgression> getQuestType() {
        return CraftQuestProgression.class;
    }
}
