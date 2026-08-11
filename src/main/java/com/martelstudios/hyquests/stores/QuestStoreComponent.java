package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.services.QuestService;

import javax.annotation.Nullable;

public class QuestStoreComponent implements Component<EntityStore> {
    public static final BuilderCodec<QuestStoreComponent> CODEC = BuilderCodec.builder(QuestStoreComponent.class, QuestStoreComponent::new)
                                                                              .append(new KeyedCodec<>("QuestStore", QuestStore.CODEC), (questStoreComponent, quests) -> questStoreComponent.questStore = quests, (questStoreComponent) -> questStoreComponent.questStore)
                                                                              .add()
                                                                              .build();
    public QuestStore questStore = new QuestStore();

    public QuestStoreComponent() {

    }

    public QuestStoreComponent(QuestStoreComponent other) {
        this.questStore = other.questStore.clone();
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new QuestStoreComponent(this);
    }

    public static ComponentType<EntityStore, QuestStoreComponent> getComponentType() {
        return QuestService.get().getPlayerStoreComponentType();
    }

    public void loadQuests() {
        questStore.loadAll();
    }
}
