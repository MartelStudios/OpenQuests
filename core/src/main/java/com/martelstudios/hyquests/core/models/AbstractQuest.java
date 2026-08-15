package com.martelstudios.hyquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.hyquests.core.assets.QuestAsset;
import com.martelstudios.hyquests.core.events.QuestUpdatedEvent;
import com.martelstudios.hyquests.core.services.PlayerQuestService;
import com.martelstudios.hyquests.core.services.QuestProgressionService;
import com.martelstudios.hyquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public abstract class AbstractQuest<Q extends AbstractQuest<Q>> {

    /**
     * Polymorphic dispatcher: concrete quest codecs register under a {@code "Type"} tag.
     */
    public static final CodecMapCodec<AbstractQuest<?>> CODEC = new CodecMapCodec<>("Type");

    private static final KeyedCodec<UUID[]> PLAYERS_CODEC = new KeyedCodec<>("Players", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));
    private static final BiConsumer<AbstractQuest, UUID[]> PLAYERS_SETTER = (quest, uuids) -> ((AbstractQuest<?>) quest).players.addAll(List.of(uuids));
    private static final Function<AbstractQuest, UUID[]> PLAYERS_GETTER = (quest) -> ((AbstractQuest<?>) quest).players.toArray(new UUID[0]);

    /**
     * Serializes the fields shared by every quest; concrete codecs chain from this.
     */
    public static final BuilderCodec<AbstractQuest> BASE_CODEC = BuilderCodec.abstractBuilder(AbstractQuest.class)
                                                                             .append(new KeyedCodec<>("Id", Codec.UUID_BINARY), (quest, uuid) -> quest.id = uuid, quest -> quest.id)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("AssetId", Codec.STRING), (quest, assetId) -> quest.assetId = assetId, quest -> quest.assetId)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("State", new EnumCodec<>(QuestState.class)), (quest, state) -> quest.state = state, quest -> quest.state)
                                                                             .add()
                                                                             .append(PLAYERS_CODEC, PLAYERS_SETTER, PLAYERS_GETTER)
                                                                             .add()
                                                                             .build();

    /**
     * Unique identity used to reference and persist this quest. Overwritten on load.
     */
    protected UUID id = UUID.randomUUID();

    /**
     * Ids of the players this quest is assigned to.
     */
    protected Set<UUID> players = ConcurrentHashMap.newKeySet();

    protected String assetId;
    protected QuestState state = QuestState.IN_PROGRESS;

    /**
     * Set when the runtime state changed; drives incremental disk saves. Not serialized.
     */
    private transient boolean dirty;

    public void update(QuestVisitor<Q> visitor) {
        visitor.progress(self());

        if (hasChanges()) {
            HytaleServer.get()
                        .getEventBus()
                        .dispatchFor(QuestUpdatedEvent.class, getId())
                        .dispatch(new QuestUpdatedEvent(this));
        }

        if (isCompleted()) {
            QuestProgressionService.get().completeQuest(getId());
        }
    }

    /**
     * Sets up whatever this quest owns beyond its own state, just after it entered the store.
     * Called by {@link QuestProgressionService#registerQuest}.
     */
    public void onRegistered() {}

    /**
     * Releases anything this quest owns beyond its own state, just after it leaves the store.
     * Called by {@link QuestProgressionService#unregisterQuest}; implementations must stay safe to
     * call on an already-unregistered quest.
     */
    public void onUnregistered() {
        for (UUID playerId : players) {
            PlayerQuestService.get().removeQuestFromPlayerStore(getId(), playerId);
        }
    }

    public void addPlayer(@Nonnull UUID playerId) {
        if (!getPlayers().add(playerId)) return;
        markDirty();

        PlayerQuestService.get().addQuestToPlayerStore(getId(), playerId);
        onPlayerAdded(playerId);
    }

    public void removePlayer(@Nonnull UUID playerId) {
        if (!getPlayers().remove(playerId)) return;
        markDirty();

        PlayerQuestService.get().removeQuestFromPlayerStore(getId(), playerId);
        onPlayerRemoved(playerId);
    }

    /**
     * Hooks for composite quests to propagate an assignment they do not own directly.
     */
    protected void onPlayerAdded(@Nonnull UUID playerId) {}

    protected void onPlayerRemoved(@Nonnull UUID playerId) {}

    public boolean isSuccessful() {
        return state == QuestState.SUCCESSFUL;
    }

    public boolean isFailed() {
        return state == QuestState.FAILED;
    }

    public boolean isAbandoned() {
        return state == QuestState.ABANDONED;
    }

    public boolean isCompleted() {
        return isSuccessful() || isFailed() || isAbandoned();
    }

    public QuestAsset getAsset() {
        return QuestAsset.getAsset(assetId);
    }

    public UUID getId() {
        return id;
    }

    public String getAssetId() {
        return assetId;
    }

    public Q setAssetId(String assetId) {
        this.assetId = assetId;
        return self();
    }

    /**
     * @return the live, mutable set of ids of the players this quest is assigned to.
     */
    public Set<UUID> getPlayers() {
        return players;
    }

    public QuestState getState() {
        return state;
    }

    public Message getTitle() {
        return Message.translation(getAsset().getTitleKey());
    }

    public Message getDescription() {
        return Message.translation(getAsset().getDescriptionKey());
    }

    public Q setState(QuestState state) {
        this.state = state;
        return self();
    }

    @SuppressWarnings("unchecked")
    protected Q self() {
        return (Q) this;
    }

    /**
     * Marks this quest as needing to be persisted on the next save pass.
     */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * @return {@code true} if this quest changed since the last save, without clearing the flag.
     */
    public boolean hasChanges() {
        return dirty;
    }

    /**
     * @return {@code true} if this quest changed since the last call, clearing the
     * flag. Mirrors {@code Objective.consumeDirty()} so saves skip untouched quests.
     */
    public boolean consumeChanges() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }
}
