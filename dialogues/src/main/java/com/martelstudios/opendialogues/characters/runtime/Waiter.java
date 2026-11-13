package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * An NPC waiting for a claim slot, with the world whose thread may touch it. Known by its ref alone,
 * so the system removing it forgets it without knowing its world.
 */
public record Waiter(@Nonnull Ref<EntityStore> ref, @Nonnull World world) {

    @Override
    public boolean equals(Object other) {
        return other instanceof Waiter waiter && waiter.ref.equals(ref);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }
}
