package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.services.QuestProgressionService;

import javax.annotation.Nullable;

public class QuestHistoryStoreComponent implements Component<EntityStore> {
    public static final BuilderCodec<QuestHistoryStoreComponent> CODEC = BuilderCodec.builder(QuestHistoryStoreComponent.class, QuestHistoryStoreComponent::new)
                                                                                     .append(new KeyedCodec<>("QuestHistoryStore", QuestHistoryStore.CODEC), (component, store) -> component.questHistoryStore = store, component -> component.questHistoryStore)
                                                                                     .add()
                                                                                     .build();

    public QuestHistoryStore questHistoryStore = new QuestHistoryStore();

    public QuestHistoryStoreComponent() {}

    public QuestHistoryStoreComponent(QuestHistoryStoreComponent other) {
        this.questHistoryStore = other.questHistoryStore.clone();
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new QuestHistoryStoreComponent(this);
    }

    public static ComponentType<EntityStore, QuestHistoryStoreComponent> getComponentType() {
        return QuestProgressionService.get().getPlayerHistoryStoreComponentType();
    }
}
