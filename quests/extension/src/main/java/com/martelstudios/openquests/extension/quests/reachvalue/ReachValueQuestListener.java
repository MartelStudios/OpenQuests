package com.martelstudios.openquests.extension.quests.reachvalue;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.extension.listener.QuestListenerComponent;

import javax.annotation.Nonnull;

/**
 * The reach value quests a player is running, so that only players with one are checked every tick.
 */
public class ReachValueQuestListener extends QuestListenerComponent {

    /**
     * @return the type registered by {@link ReachValueFeature}, {@code null} before it registers.
     */
    public static ComponentType<EntityStore, ReachValueQuestListener> getComponentType() {
        return ReachValueFeature.getListenerType();
    }

    @Nonnull
    @Override
    protected QuestListenerComponent create() {
        return new ReachValueQuestListener();
    }
}
