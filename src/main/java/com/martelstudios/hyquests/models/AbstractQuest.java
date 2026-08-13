package com.martelstudios.hyquests.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.assets.QuestAsset;
import com.martelstudios.hyquests.events.QuestUpdatedEvent;
import com.martelstudios.hyquests.services.PlayerQuestService;
import com.martelstudios.hyquests.services.QuestProgressionService;
import com.martelstudios.hyquests.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import java.util.*;
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
                                                                             .append(new KeyedCodec<>("QuestAssetId", Codec.STRING), (quest, assetId) -> quest.questAssetId = assetId, quest -> quest.questAssetId)
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

    protected String questAssetId;
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
        removeAllPlayers();
    }

    public void addPlayersFromPlayerRef(@Nonnull Collection<PlayerRef> playerRefs) {
        playerRefs.forEach(this::addPlayer);
    }

    public void addPlayer(@Nonnull PlayerRef playerRef) {
        addPlayer(playerRef.getUuid());
    }

    public void addPlayer(@Nonnull UUID playerId) {
        if (!getPlayers().add(playerId)) return;
        markDirty();

        PlayerRef online = Universe.get().getPlayer(playerId);
        if (online != null) {
            var ref = online.getReference();
            if (ref != null) {
                var world = ref.getStore().getExternalData().getWorld();
                world.execute(() -> PlayerQuestService.get().addQuestToPlayerStore(getId(), ref));
            }
        } else {
            Universe.get()
                    .getPlayerStorage()
                    .update(playerId, holder -> PlayerQuestService.get().addQuestToPlayerStore(getId(), holder));
        }

        onPlayerAdded(playerId);
    }

    /**
     * Assigns this quest through an already-resolved holder, for callers that hold one. A
     * connecting player is neither online yet nor safe to reach through stored data, so their
     * incoming holder is the only handle that sticks.
     */
    public void addPlayer(@Nonnull UUID playerId, @Nonnull Holder<EntityStore> holder) {
        if (!getPlayers().add(playerId)) return;
        markDirty();

        PlayerQuestService.get().addQuestToPlayerStore(getId(), holder);
        onPlayerAdded(playerId);
    }

    public void removePlayer(@Nonnull UUID playerId) {
        if (!getPlayers().remove(playerId)) return;
        markDirty();

        PlayerRef online = Universe.get().getPlayer(playerId);
        if (online != null) {
            var ref = online.getReference();
            if (ref != null) {
                var world = ref.getStore().getExternalData().getWorld();
                world.execute(() -> PlayerQuestService.get().removeQuestFromPlayerStore(getId(), ref));
            }
        } else {
            Universe.get()
                    .getPlayerStorage()
                    .update(playerId, holder -> PlayerQuestService.get().removeQuestFromPlayerStore(getId(), holder));
        }

        onPlayerRemoved(playerId);
    }

    public void removeAllPlayers() {
        new ArrayList<>(getPlayers()).forEach(this::removePlayer);
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
        return QuestAsset.getAsset(questAssetId);
    }

    public UUID getId() {
        return id;
    }

    public String getQuestAssetId() {
        return questAssetId;
    }

    public Q setQuestAssetId(String questAssetId) {
        this.questAssetId = questAssetId;
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
