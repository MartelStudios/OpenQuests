package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.HyQuestsPlugin;

import javax.annotation.Nullable;

public class QuestHistoryStoreComponent implements Component<EntityStore> {
    public static final BuilderCodec<QuestHistoryStoreComponent> CODEC = BuilderCodec.builder(QuestHistoryStoreComponent.class, QuestHistoryStoreComponent::new)
                                                                                     .append(new KeyedCodec<>("QuestHistoryStore", QuestHistoryStore.CODEC), (component, store) -> component.history = store, component -> component.history)
                                                                                     .add()
                                                                                     .build();

    public QuestHistoryStore history = new QuestHistoryStore();

    public QuestHistoryStoreComponent() {}

    public QuestHistoryStoreComponent(QuestHistoryStoreComponent other) {
        this.history = other.history.clone();
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new QuestHistoryStoreComponent(this);
    }

    public static ComponentType<EntityStore, QuestHistoryStoreComponent> getComponentType() {
        return HyQuestsPlugin.get().getQuestHistoryStoreComponentType();
    }
}
