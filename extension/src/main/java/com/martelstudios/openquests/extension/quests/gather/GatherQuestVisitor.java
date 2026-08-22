package com.martelstudios.openquests.extension.quests.gather;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Progresses {@link GatherQuestProgression}s on inventory change.
 */
public class GatherQuestVisitor implements QuestVisitor<GatherQuestProgression> {
    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final UUID playerId;
    private final CombinedItemContainer combinedItemContainer;

    public GatherQuestVisitor(@Nonnull EntityComponents playerComponents) {
        this.playerId = playerComponents.getComponent(UUIDComponent.getComponentType()).getUuid();

        List<ItemContainer> containers = new ArrayList<>(InventoryComponent.EVERYTHING.length);
        for (var inventoryType : InventoryComponent.EVERYTHING) {
            var inventory = playerComponents.getComponent(inventoryType);
            if (inventory != null) containers.add(inventory.getInventory());
        }

        this.combinedItemContainer = new CombinedItemContainer(containers.toArray(new ItemContainer[0]));
    }

    @Override
    public void progress(GatherQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted()) return;

        int count = combinedItemContainer.countItemStacks(itemStack -> quest.getItemToGather()
                                                                            .isBlockTypeIncluded(itemStack.getItemId()));

        quest.setCurrentQuantity(count)
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();

        LOGGER.at(Level.FINE)
              .log("Quest %s progress for %s: %d/%d", quest.getId(), playerId, quest.getCurrentQuantity(), quest.getTargetQuantity());
    }

    @Override
    public Class<GatherQuestProgression> getQuestType() {
        return GatherQuestProgression.class;
    }
}
