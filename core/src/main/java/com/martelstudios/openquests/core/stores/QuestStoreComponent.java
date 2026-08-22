package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestCorePlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class QuestStoreComponent implements Component<EntityStore> {
    public static final BuilderCodec<QuestStoreComponent> CODEC = BuilderCodec.builder(QuestStoreComponent.class, QuestStoreComponent::new)
                                                                              .append(new KeyedCodec<>("QuestStore", QuestsRecord.CODEC), (questStoreComponent, quests) -> questStoreComponent.questsRecord = quests, (questStoreComponent) -> questStoreComponent.questsRecord)
                                                                              .add()
                                                                              .append(new KeyedCodec<>("StartedOnConnection", new SetCodec<>(Codec.STRING, HashSet<String>::new, false)), (component, ids) -> component.startedOnConnection.addAll(ids), component -> component.startedOnConnection)
                                                                              .add()
                                                                              .build();
    public QuestsRecord questsRecord = new QuestsRecord();

    /**
     * Asset ids already handed to this player by {@code StartOnConnection}. Only this set is kept
     * between sessions, not the quests themselves, so a catalogue offered to everyone costs one
     * string per quest actually taken.
     */
    private final Set<String> startedOnConnection = ConcurrentHashMap.newKeySet();

    public QuestStoreComponent() {

    }

    public QuestStoreComponent(QuestStoreComponent other) {
        this.questsRecord = other.questsRecord.clone();
        this.startedOnConnection.addAll(other.startedOnConnection);
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new QuestStoreComponent(this);
    }

    public static ComponentType<EntityStore, QuestStoreComponent> getComponentType() {
        return OpenQuestCorePlugin.get().getQuestStoreComponentType();
    }

    public void loadQuests() {
        questsRecord.loadAll();
    }

    @Nonnull
    public Set<String> getStartedOnConnection() {
        return startedOnConnection;
    }
}
