package com.martelstudios.openquests.extension.quests.stat;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.extension.listener.QuestListenerComponent;

import javax.annotation.Nonnull;

/**
 * The lose stat quests a player is running, so the ticking system reaches only those players.
 */
public class LoseStatQuestListener extends QuestListenerComponent {

    /**
     * @return the component type registered by {@link LoseStatFeature}.
     */
    public static ComponentType<EntityStore, LoseStatQuestListener> getComponentType() {
        return LoseStatFeature.getListenerType();
    }

    @Nonnull
    @Override
    protected QuestListenerComponent create() {
        return new LoseStatQuestListener();
    }
}
