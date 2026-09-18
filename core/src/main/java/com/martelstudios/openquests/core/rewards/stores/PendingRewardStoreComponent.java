package com.martelstudios.openquests.core.rewards.stores;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;

import javax.annotation.Nullable;

/**
 * What one player is still owed, while they are online. Written down with the rest of their record
 * rather than in their entity file, so a debt follows them to whichever server they land on next.
 */
public class PendingRewardStoreComponent implements Component<EntityStore> {

    public PendingRewardStore pending = new PendingRewardStore();

    public PendingRewardStoreComponent() {}

    public PendingRewardStoreComponent(PendingRewardStoreComponent other) {
        this.pending = other.pending.clone();
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new PendingRewardStoreComponent(this);
    }

    public static ComponentType<EntityStore, PendingRewardStoreComponent> getComponentType() {
        return OpenQuestsCorePlugin.get().getPendingRewardStoreComponentType();
    }
}
