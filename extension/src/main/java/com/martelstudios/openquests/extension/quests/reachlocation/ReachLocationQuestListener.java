package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.extension.listener.QuestListenerComponent;

import javax.annotation.Nonnull;

/**
 * The reach-location quests a player is running, so that only players with one are checked against
 * a position every tick.
 */
public class ReachLocationQuestListener extends QuestListenerComponent {

    public static ComponentType<EntityStore, ReachLocationQuestListener> getComponentType() {
        return ReachLocationFeature.getListenerType();
    }

    @Nonnull
    @Override
    protected QuestListenerComponent create() {
        return new ReachLocationQuestListener();
    }
}
