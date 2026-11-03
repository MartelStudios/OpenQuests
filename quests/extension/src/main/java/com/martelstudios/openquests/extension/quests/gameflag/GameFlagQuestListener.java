package com.martelstudios.openquests.extension.quests.gameflag;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.extension.listener.QuestListenerComponent;

import javax.annotation.Nonnull;

/**
 * The game flag quests a player is running, so that only players with one are checked every tick.
 */
public class GameFlagQuestListener extends QuestListenerComponent {

    /**
     * @return the type registered by {@link GameFlagFeature}, {@code null} before it registers.
     */
    public static ComponentType<EntityStore, GameFlagQuestListener> getComponentType() {
        return GameFlagFeature.getListenerType();
    }

    @Nonnull
    @Override
    protected QuestListenerComponent create() {
        return new GameFlagQuestListener();
    }
}
