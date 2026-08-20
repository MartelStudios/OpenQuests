package com.martelstudios.hyquests.extension.quests.interactivelypickup;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.martelstudios.hyquests.core.models.QuestState;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;

import java.util.UUID;
import java.util.logging.Level;
import javax.annotation.Nonnull;

/**
 * Progresses {@link InteractivelyPickupQuestProgression}s by accumulating what a single harvest interaction
 * yielded, unlike gather quests which recount the whole inventory.
 */
public class InteractivelyPickupQuestVisitor implements QuestVisitor<InteractivelyPickupQuestProgression> {
    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final UUID playerId;
    private final InteractivelyPickupItemEvent event;

    public InteractivelyPickupQuestVisitor(UUID playerId, InteractivelyPickupItemEvent event) {
        this.playerId = playerId;
        this.event = event;
    }

    @Override
    public void progress(InteractivelyPickupQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.getState() == QuestState.SUCCESSFUL) return;


        if (!quest.getItemToPickup().isBlockTypeIncluded(event.getItemStack().getItemId())) {
            return;
        }

        quest.setCount(quest.getCount() + event.getItemStack().getQuantity())
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();

        LOGGER.at(Level.FINE).log("Quest %s progress for %s: %d/%d", quest.getId(), playerId, quest.getCount(), quest.getTargetCount());
    }

    @Override
    public Class<InteractivelyPickupQuestProgression> getQuestType() {
        return InteractivelyPickupQuestProgression.class;
    }
}
