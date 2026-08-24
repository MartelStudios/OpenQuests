package com.martelstudios.openquests.extension.quests.enterworld;

import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;

/**
 * Needs no system: entering a world is already an event, and it is the only thing this type
 * reacts to.
 */
public final class EnterWorldQuestEvents {

    private EnterWorldQuestEvents() {}

    public static void handleAddPlayerToWorld(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var playerRef = addPlayerToWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        var questStore = addPlayerToWorldEvent.getHolder().getComponent(QuestStoreComponent.getComponentType());
        if (playerRef == null || questStore == null) return;

        QuestProgressionService.get()
                               .progress(new EnterWorldQuestVisitor(playerRef.getUuid(), addPlayerToWorldEvent.getWorld().getName()), questStore.getQuestIds());
    }
}
