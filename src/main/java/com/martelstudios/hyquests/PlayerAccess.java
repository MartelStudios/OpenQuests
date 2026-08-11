package com.martelstudios.hyquests;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Reads and writes a player's components without caring whether they are online. Needed because
 * the two paths are incompatible: a live player is reached through {@code Store} + {@code Ref},
 * while an offline one is a detached {@link Holder}, and writing to the wrong one is silently
 * discarded.
 */
public interface PlayerAccess {

    /**
     * Wraps a live player. Its methods must be called on the ref's world thread.
     */
    @Nonnull
    static PlayerAccess of(@Nonnull Ref<EntityStore> ref) {
        return new PlayerAccess() {
            @Nullable
            @Override
            public <T extends Component<EntityStore>> T getComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
                return ref.getStore().getComponent(ref, componentType);
            }

            @Nonnull
            @Override
            public <T extends Component<EntityStore>> T ensureAndGetComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
                return ref.getStore().ensureAndGetComponent(ref, componentType);
            }
        };
    }

    /**
     * Wraps detached player data: either an offline player loaded by {@code PlayerStorage#update},
     * or one being connected, before their entity joins a world.
     */
    @Nonnull
    static PlayerAccess of(@Nonnull Holder<EntityStore> holder) {
        return new PlayerAccess() {
            @Nullable
            @Override
            public <T extends Component<EntityStore>> T getComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
                return holder.getComponent(componentType);
            }

            @Nonnull
            @Override
            public <T extends Component<EntityStore>> T ensureAndGetComponent(@Nonnull ComponentType<EntityStore, T> componentType) {
                return holder.ensureAndGetComponent(componentType);
            }
        };
    }

    @Nullable
    <T extends Component<EntityStore>> T getComponent(@Nonnull ComponentType<EntityStore, T> componentType);

    @Nonnull
    <T extends Component<EntityStore>> T ensureAndGetComponent(@Nonnull ComponentType<EntityStore, T> componentType);
}
