package com.martelstudios.hyquests.visitors;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.martelstudios.hyquests.models.InteractivelyPickupQuest;
import com.martelstudios.hyquests.models.QuestState;

import java.util.UUID;

public class InteractivelyPickupQuestVisitor implements QuestVisitor<InteractivelyPickupQuest> {
    private final UUID playerId;
    private final InteractivelyPickupItemEvent event;

    public InteractivelyPickupQuestVisitor(UUID playerId, InteractivelyPickupItemEvent event) {
        this.playerId = playerId;
        this.event = event;
    }

    @Override
    public void progress(InteractivelyPickupQuest quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.getState() == QuestState.SUCCESSFUL) return;

        var asset = quest.getAsset();
        if (!asset.getItemToPickup().equals(event.getItemStack().getItemId())) {
            return;
        }

        quest.setCount(quest.getCount() + event.getItemStack().getQuantity())
             .setState(quest.checkCompletion() ? QuestState.SUCCESSFUL : QuestState.IN_PROGRESS)
             .markDirty();
        ConsoleSender.INSTANCE.sendMessage(Message.raw("Quest progress: " + quest.getCount() + "/" + quest.getAsset()
                                                                                                          .getCount()));
    }

    @Override
    public Class<InteractivelyPickupQuest> getQuestType() {
        return InteractivelyPickupQuest.class;
    }
}
