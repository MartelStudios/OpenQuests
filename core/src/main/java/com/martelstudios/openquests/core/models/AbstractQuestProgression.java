package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Defines the quest progression. Extend it to create new quest types or to add runtime progression data.
 * Avoid using it to declare static serialized data. Look at {@link QuestAsset} for static serialized data declaration.
 */
public abstract class AbstractQuestProgression<Q extends AbstractQuestProgression<Q>> {
    public static final CodecMapCodec<AbstractQuestProgression<?>> CODEC = new CodecMapCodec<>("Type");

    private static final KeyedCodec<UUID[]> PLAYERS_CODEC = new KeyedCodec<>("Players", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));
    private static final BiConsumer<AbstractQuestProgression, UUID[]> PLAYERS_SETTER = (quest, uuids) -> ((AbstractQuestProgression<?>) quest).players.addAll(List.of(uuids));
    private static final Function<AbstractQuestProgression, UUID[]> PLAYERS_GETTER = (quest) -> ((AbstractQuestProgression<?>) quest).players.toArray(new UUID[0]);

    /**
     * Serializes the fields shared by every quest progression; concrete codecs chain from this.
     */
    public static final BuilderCodec<AbstractQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(AbstractQuestProgression.class)
                                                                                        .append(new KeyedCodec<>("Id", Codec.UUID_BINARY), (quest, uuid) -> quest.id = uuid, quest -> quest.id)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("AssetId", Codec.STRING), (quest, assetId) -> quest.assetId = assetId, quest -> quest.assetId)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("State", new EnumCodec<>(QuestState.class)), (quest, state) -> quest.state = state, quest -> quest.state)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("PersistHistory", Codec.BOOLEAN), (quest, value) -> quest.persistHistory = value, quest -> quest.persistHistory)
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

    /**
     * The {@link QuestAsset#getId()}
     */
    protected String assetId;

    protected QuestState state = QuestState.IN_PROGRESS;

    /**
     * Overrides the asset for this instance alone. Boxed so that "not overridden" is a state of
     * its own, and so the codec leaves it out entirely.
     */
    @Nullable
    protected Boolean persistHistory;

    /**
     * Set when the runtime state changed; drives incremental disk saves. Not serialized.
     */
    private transient boolean dirty;

    /**
     * @return whether reaching a terminal state ends this quest. One that says {@code false} keeps
     * running and being re-evaluated, so its state can still change.
     */
    public boolean isStopOnComplete() {
        QuestAsset asset = getAsset();
        return asset == null || asset.isStopOnComplete();
    }

    /**
     * @return whether completing this quest is recorded in its players history. A quest handed out
     * as part of another one can be told not to, whatever its asset says.
     */
    public boolean isPersistHistory() {
        if (persistHistory != null) return persistHistory;

        QuestAsset asset = getAsset();
        return asset == null || asset.isPersistHistory();
    }

    /**
     * @return whether a player may give this quest up. Falls back to allowing it when the asset
     * is gone, so a quest nobody can name is not also one nobody can be rid of.
     */
    public boolean canBeAbandoned() {
        QuestAsset asset = getAsset();

        return asset == null || asset.canBeAbandoned();
    }

    public Q setPersistHistory(@Nullable Boolean persistHistory) {
        this.persistHistory = persistHistory;
        return self();
    }

    /**
     * Updates the quest progression by applying the visitor to it.
     * After the visitor's pass, it looks for changes to notify and for quest completion.
     *
     * @param visitor the visitor to apply
     */
    public void update(QuestVisitor<Q> visitor) {
        visitor.progress(self());

        if (hasChanges()) {
            HytaleServer.get()
                        .getEventBus()
                        .dispatchFor(QuestUpdatedEvent.class, getId())
                        .dispatch(new QuestUpdatedEvent(this));
        }

        if (isCompleted() && isStopOnComplete()) {
            QuestProgressionService.get().completeQuest(getId());
        }
    }

    /**
     * Called just after the quest progression entered the quest store.
     * Called by {@link QuestProgressionService#registerQuest(AbstractQuestProgression)}.
     */
    public void onRegistered() {}

    /**
     * Called just after the quest progression leaves the quest store.
     * Called by {@link QuestProgressionService#unregisterQuest}.
     */
    public void onUnregistered() {}

    /**
     * @return {@code false} if the player already held this quest.
     */
    public boolean addPlayer(@Nonnull UUID playerId) {
        if (!getPlayers().add(playerId)) return false;
        markDirty();

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestPlayerAddedEvent.class, playerId)
                    .dispatch(new QuestPlayerAddedEvent(this, playerId));

        return true;
    }

    /**
     * @return {@code false} if the player did not hold this quest.
     */
    public boolean removePlayer(@Nonnull UUID playerId) {
        if (!getPlayers().remove(playerId)) return false;
        markDirty();

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestPlayerRemovedEvent.class, playerId)
                    .dispatch(new QuestPlayerRemovedEvent(this, playerId));

        return true;
    }

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

    public Q setState(QuestState state) {
        this.state = state;
        return self();
    }

    /**
     * @return the title of the asset, or what the type makes of itself when the asset names none.
     */
    @Nonnull
    public Message getTitle() {
        QuestAsset asset = getAsset();
        String titleKey = asset == null ? null : asset.getTitleKey();

        return titleKey != null ? Message.translation(titleKey) : getDefaultTitle();
    }

    @Nonnull
    public Message getDescription() {
        QuestAsset asset = getAsset();
        String descriptionKey = asset == null ? null : asset.getDescriptionKey();

        return descriptionKey != null ? Message.translation(descriptionKey) : getDefaultDescription();
    }

    /**
     * Stands in when the asset names no title. A type that can describe itself from its own
     * parameters overrides this, so an unnamed quest still reads as something.
     */
    @Nonnull
    public Message getDefaultTitle() {
        return Message.translation("openquests.quest.default.title");
    }

    /**
     * Empty rather than a stand-in.
     */
    @Nonnull
    public Message getDefaultDescription() {
        return Message.raw("");
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

    /**
     * The title of a quest asset. If the asset has no title, the quest is instantiated to read the default one.
     */
    @Nonnull
    public static Message titleOf(@Nonnull QuestAsset asset) {
        String titleKey = asset.getTitleKey();

        return titleKey != null ? Message.translation(titleKey) : asset.create().getDefaultTitle();
    }
}
