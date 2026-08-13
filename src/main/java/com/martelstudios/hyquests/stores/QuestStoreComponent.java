package com.martelstudios.hyquests.stores;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.HyQuestsPlugin;

import javax.annotation.Nullable;

public class QuestStoreComponent implements Component<EntityStore> {
    public static final BuilderCodec<QuestStoreComponent> CODEC = BuilderCodec.builder(QuestStoreComponent.class, QuestStoreComponent::new)
                                                                              .append(new KeyedCodec<>("QuestStore", QuestsRecord.CODEC), (questStoreComponent, quests) -> questStoreComponent.questsRecord = quests, (questStoreComponent) -> questStoreComponent.questsRecord)
                                                                              .add()
                                                                              .build();
    public QuestsRecord questsRecord = new QuestsRecord();

    public QuestStoreComponent() {

    }

    public QuestStoreComponent(QuestStoreComponent other) {
        this.questsRecord = other.questsRecord.clone();
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new QuestStoreComponent(this);
    }

    public static ComponentType<EntityStore, QuestStoreComponent> getComponentType() {
        return HyQuestsPlugin.get().getQuestStoreComponentType();
    }

    public void loadQuests() {
        questsRecord.loadAll();
    }
}
