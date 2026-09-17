package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.extension.listener.QuestListenerComponent;

import javax.annotation.Nonnull;

/**
 * The movement quests a player is running, whatever their pace and whether they count steps or
 * jumps — the kind is registered on {@link MovementQuestProgression}, so a quest type built on it
 * elsewhere is followed here too without saying anything.
 */
public class MovementQuestListener extends QuestListenerComponent {

    public static ComponentType<EntityStore, MovementQuestListener> getComponentType() {
        return MovementFeature.getListenerType();
    }

    @Nonnull
    @Override
    protected QuestListenerComponent create() {
        return new MovementQuestListener();
    }
}
