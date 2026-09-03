package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.set.SetCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The quests of one player. Every quest they take part in is indexed here; those they are alone in
 * are also held whole, so they need no file of their own.
 */
public class QuestStoreComponent implements Component<EntityStore> {
    public static final BuilderCodec<QuestStoreComponent> CODEC = BuilderCodec.builder(QuestStoreComponent.class, QuestStoreComponent::new)
                                                                              .append(new KeyedCodec<>("SharedQuests", QuestsRecord.CODEC), (component, quests) -> component.quests = quests, QuestStoreComponent::getSharedQuests)
                                                                              .add()
                                                                              .append(new KeyedCodec<>("OwnQuests", new ArrayCodec<>(AbstractQuestProgression.CODEC, AbstractQuestProgression<?>[]::new)), QuestStoreComponent::setOwnQuests, QuestStoreComponent::getOwnQuestsArray)
                                                                              .add()
                                                                              .append(new KeyedCodec<>("StartedOnConnection", new SetCodec<>(Codec.STRING, HashSet<String>::new, false)), (component, ids) -> component.startedOnConnection.addAll(ids), component -> component.startedOnConnection)
                                                                              .add()
                                                                              .build();

    /**
     * Every quest of this player, whichever store holds it. Decoded from the shared half alone,
     * the own quests putting their ids back as they are read.
     */
    private QuestsRecord quests = new QuestsRecord();

    /**
     * Progressions this player is responsible for persisting, kept whole rather than as a file of
     * their own.
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
        this.quests = other.quests.clone();
        this.ownQuests.putAll(other.ownQuests);
        this.startedOnConnection.addAll(other.startedOnConnection);
    }

    @Nullable
    @Override
    public Component<EntityStore> clone() {
        return new QuestStoreComponent(this);
    }

    public static ComponentType<EntityStore, QuestStoreComponent> getComponentType() {
        return OpenQuestsCorePlugin.get().getQuestStoreComponentType();
    }

    /**
     * @return the index of every quest of this player, own ones included.
     */
    @Nonnull
    public QuestsRecord getQuests() {
        return quests;
    }

    /**
     * @return the live set of ids of every quest of this player.
     */
    @Nonnull
    public Set<UUID> getQuestIds() {
        return quests.getAllIds();
    }

    public void loadQuests() {
        quests.loadAll();
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

    /**
     * @return the progressions this player persists, by id.
     */
    @Nonnull
    public Map<UUID, AbstractQuestProgression<?>> getOwnQuests() {
        return ownQuests;
    }

    @Nonnull
    public Set<String> getStartedOnConnection() {
        return startedOnConnection;
    }

    /**
     * Writes back only what this player holds alone. A quest that gained a second player is dropped
     * here and picked up by the quest store's own files, which is the whole migration.
     */
    @Nonnull
    private AbstractQuestProgression<?>[] getOwnQuestsArray() {
        return ownQuests.values()
                        .stream()
                        .filter(p -> shouldBeHeldByOwner(p) && isPersisted(p))
                        .toArray(AbstractQuestProgression<?>[]::new);
    }

    private void setOwnQuests(@Nonnull AbstractQuestProgression<?>[] quests) {
        for (AbstractQuestProgression<?> quest : quests) {
            ownQuests.put(quest.getId(), quest);
            this.quests.register(quest.getId());
        }
    }

    /**
     * The ids left to resolve from elsewhere. Quests written whole below are left out, and so are
     * the ones no store will write: their id would only resolve to nothing next session.
     */
    @Nonnull
    private QuestsRecord getSharedQuests() {
        QuestsRecord shared = new QuestsRecord();

        for (UUID questId : quests.getAllIds()) {
            AbstractQuestProgression<?> quest = ownQuests.get(questId);
            if (quest == null) quest = QuestProgressionService.get().getQuest(questId);

            if (quest != null && (shouldBeHeldByOwner(quest) || !isPersisted(quest))) continue;

            shared.register(questId);
        }
        return shared;
    }

    /**
     * @return {@code true} if this quest is written with its only player rather than in a file.
     */
    private static boolean shouldBeHeldByOwner(@Nonnull AbstractQuestProgression<?> quest) {
        return quest.getPlayers().size() <= 1;
    }

    /**
     * @return {@code true} if this quest is meant to survive a restart at all.
     */
    private static boolean isPersisted(@Nonnull AbstractQuestProgression<?> quest) {
        QuestAsset asset = quest.getAsset();
        return asset == null || asset.isPersistProgression();
    }
}
