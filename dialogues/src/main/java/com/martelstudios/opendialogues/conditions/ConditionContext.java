package com.martelstudios.opendialogues.conditions;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.history.DialogueHistory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Who a condition is judged for: the player, their entity when it can be read, and what they did
 * with dialogues so far.
 *
 * @param entity {@code null} for a player between worlds, when only the history can be judged.
 */
public record ConditionContext(@Nullable PlayerRef player, @Nullable Ref<EntityStore> entity, @Nullable Store<EntityStore> store, @Nonnull DialogueHistory history) {

    /**
     * @return whether the entity can be read here, which only its world thread may do.
     */
    public boolean canReadEntity() {
        return entity != null && entity.isValid() && store != null && store.isInThread();
    }
}
