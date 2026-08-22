package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestCorePlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestStoreComponent implements Component<EntityStore> {
    public static final BuilderCodec<QuestStoreComponent> CODEC = BuilderCodec.builder(QuestStoreComponent.class, QuestStoreComponent::new)
                                                                              .append(new KeyedCodec<>("QuestStore", QuestsRecord.CODEC), (questStoreComponent, quests) -> questStoreComponent.questsRecord = quests, (questStoreComponent) -> questStoreComponent.questsRecord)
                                                                              .add()
                                                                              .append(new KeyedCodec<>("OwnQuests", new ArrayCodec<>(AbstractQuestProgression.CODEC, AbstractQuestProgression<?>[]::new)), QuestStoreComponent::setOwnQuests, QuestStoreComponent::getOwnQuestsArray)
                                                                              .add()
                                                                              .append(new KeyedCodec<>("StartedOnConnection", new SetCodec<>(Codec.STRING, HashSet<String>::new, false)), (component, ids) -> component.startedOnConnection.addAll(ids), component -> component.startedOnConnection)
                                                                              .add()
                                                                              .build();
    public QuestsRecord questsRecord = new QuestsRecord();

    /**
     * Progressions this player is responsible for persisting, kept whole rather than as a file of
     * their own. Only those still held by this player alone are written back.
     */
    private final Map<UUID, AbstractQuestProgression<?>> ownQuests = new ConcurrentHashMap<>();

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
        this.ownQuests.putAll(other.ownQuests);
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

    /**
     * Takes over persisting a quest. Doing so is harmless for a shared one: what is written back
     * is decided at encode time, not here.
     */
    public void addOwnQuest(@Nonnull AbstractQuestProgression<?> quest) {
        ownQuests.put(quest.getId(), quest);
    }

    public void removeOwnQuest(@Nonnull UUID questId) {
        ownQuests.remove(questId);
    }

    @Nonnull
    public Map<UUID, AbstractQuestProgression<?>> getOwnQuests() {
        return ownQuests;
    }

    @Nonnull
    public Set<String> getStartedOnConnection() {
        return startedOnConnection;
    }

    private void setOwnQuests(@Nonnull AbstractQuestProgression<?>[] quests) {
        for (AbstractQuestProgression<?> quest : quests) {
            ownQuests.put(quest.getId(), quest);
            questsRecord.register(quest.getId());
        }
    }

    /**
     * Writes back only what this player still holds alone. A quest that gained a second player is
     * dropped here and picked up by the quest store's own files, which is the whole migration.
     */
    @Nonnull
    private AbstractQuestProgression<?>[] getOwnQuestsArray() {
        return ownQuests.values()
                        .stream()
                        .filter(quest -> quest.getPlayers().size() == 1)
                        .toArray(AbstractQuestProgression<?>[]::new);
    }
}
