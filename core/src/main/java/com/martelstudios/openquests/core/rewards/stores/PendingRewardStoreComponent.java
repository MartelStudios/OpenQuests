package com.martelstudios.openquests.core.rewards.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;

import javax.annotation.Nullable;

public class PendingRewardStoreComponent implements Component<EntityStore> {
    public static final BuilderCodec<PendingRewardStoreComponent> CODEC = BuilderCodec.builder(PendingRewardStoreComponent.class, PendingRewardStoreComponent::new)
                                                                                      .append(new KeyedCodec<>("PendingRewardStore", PendingRewardStore.CODEC), (component, store) -> component.pending = store, component -> component.pending)
                                                                                      .add()
                                                                                      .build();

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
