package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAbandonedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
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

    private static final KeyedCodec<UUID[]> ABANDONED_PLAYERS_CODEC = new KeyedCodec<>("AbandonedPlayers", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));
    private static final BiConsumer<AbstractQuestProgression, UUID[]> ABANDONED_PLAYERS_SETTER = (quest, uuids) -> ((AbstractQuestProgression<?>) quest).abandonedPlayers.addAll(List.of(uuids));
    private static final Function<AbstractQuestProgression, UUID[]> ABANDONED_PLAYERS_GETTER = (quest) -> ((AbstractQuestProgression<?>) quest).abandonedPlayers.toArray(new UUID[0]);

    private static final KeyedCodec<String[]> TAGS_CODEC = new KeyedCodec<>("Tags", new ArrayCodec<>(Codec.STRING, String[]::new));
    private static final BiConsumer<AbstractQuestProgression, String[]> TAGS_SETTER = (quest, tags) -> ((AbstractQuestProgression<?>) quest).tags.addAll(List.of(tags));
    private static final Function<AbstractQuestProgression, String[]> TAGS_GETTER = (quest) -> ((AbstractQuestProgression<?>) quest).tags.toArray(new String[0]);

    private static final String[] NO_TAG_VALUES = new String[0];

    /**
     * Serializes the fields shared by every quest progression; concrete codecs chain from this.
     */
    public static final BuilderCodec<AbstractQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(AbstractQuestProgression.class)
                                                                                        .append(new KeyedCodec<>("Id", Codec.UUID_STRING), (quest, uuid) -> quest.id = uuid, quest -> quest.id)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("AssetId", Codec.STRING), (quest, assetId) -> quest.assetId = assetId, quest -> quest.assetId)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("State", new EnumCodec<>(QuestState.class)), (quest, state) -> quest.state = state, quest -> quest.state)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("PersistHistory", Codec.BOOLEAN), (quest, value) -> quest.persistHistory = value, quest -> quest.persistHistory)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("StartedAt", Codec.LONG), (quest, millis) -> quest.startedAt = Instant.ofEpochMilli(millis), quest -> quest.startedAt == null ? null : Long.valueOf(quest.startedAt.toEpochMilli()))
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("CompletedAt", Codec.LONG), (quest, millis) -> quest.completedAt = Instant.ofEpochMilli(millis), quest -> quest.completedAt == null ? null : Long.valueOf(quest.completedAt.toEpochMilli()))
                                                                                        .add()
                                                                                        .append(PLAYERS_CODEC, PLAYERS_SETTER, PLAYERS_GETTER)
                                                                                        .add()
                                                                                        .append(ABANDONED_PLAYERS_CODEC, ABANDONED_PLAYERS_SETTER, ABANDONED_PLAYERS_GETTER)
                                                                                        .add()
                                                                                        .append(TAGS_CODEC, TAGS_SETTER, TAGS_GETTER)
                                                                                        .add()
                                                                                        .build();

    /**
     * Unique identity used to reference and persist this quest. Overwritten on load.
     */
    protected UUID id = UUID.randomUUID();

    /**
     * Ids of the players still running the quest progression.
     */
    protected Set<UUID> players = ConcurrentHashMap.newKeySet();

    /**
     * Ids of the players who have left the quest progression.
     */
    protected Set<UUID> abandonedPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Tags written on this instance, on top of those its asset declares. What became of this one
     * quest belongs here, since an asset is shared by everyone holding it.
     */
    protected Set<String> tags = ConcurrentHashMap.newKeySet();

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
     * When the player was handed this quest, written once as it enters the store. Null for a quest
     * registered before this was kept, which is a date nobody can invent after the fact.
     */
    @Nullable
    protected Instant startedAt;

    /**
     * When it reached the outcome it now carries. Rewritten whenever that outcome changes, and
     * struck out if the quest goes back to running — a quest kept alive by {@code StopOnComplete:
     * false} can, and a date left standing would say it ended when it did not.
     */
    @Nullable
    protected Instant completedAt;

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
     * After the visitor's pass, the quest settles before anyone hears about it.
     *
     * @param visitor the visitor to apply
     */
    public void update(QuestVisitor<Q> visitor) {
        QuestState previousState = getState();

        visitor.progress(self());

        // If no player remains and some has abandoned set the quest as abandoned
        if (!isCompleted() && players.isEmpty() && !abandonedPlayers.isEmpty()) {
            setState(QuestState.ABANDONED).markDirty();
        }

        if (hasChanges()) {
            HytaleServer.get()
                        .getEventBus()
                        .dispatchFor(QuestUpdatedEvent.class, getId())
                        .dispatch(new QuestUpdatedEvent(this));
        }

        // The outcome, not merely the first end: a quest kept alive by StopOnComplete:false can change.
        if (isCompleted() && getState() != previousState) {
            completedAt = isCompleted() ? Instant.now() : null;
            markDirty();

            HytaleServer.get()
                        .getEventBus()
                        .dispatchFor(QuestCompletedEvent.class, getId())
                        .dispatch(new QuestCompletedEvent(this));
        }
    }

    /**
     * Called just after the quest progression entered the quest store.
     * Called by {@link QuestProgressionService#registerQuest(AbstractQuestProgression)}.
     *
     * <p>Runs once, when the quest is first handed out, and not when one is read back from disk —
     * which is what makes it the moment the quest started.
     */
    public void onRegistered() {
        startedAt = Instant.now();
        markDirty();
    }

    /**
     * Called just after the quest ended and was set aside, still answerable by id but no longer
     * running. Called by {@link QuestProgressionService#archiveQuest}.
     *
     * <p>Where a quest made of others settles them: they cannot get anywhere once it is over, and
     * the archive is meant to be read whole.
     */
    public void onArchived() {}

    /**
     * Called just after the quest progression leaves the quest store for good.
     * Called by {@link QuestProgressionService#unregisterQuest}.
     */
    public void onUnregistered() {}

    /**
     * Add the player to the quest progression
     * @return {@code false} if the player already held this quest.
     */
    public boolean addPlayer(@Nonnull UUID playerId) {
        if (!getPlayers().add(playerId)) return false;

        // Handed the quest again after walking away from it: they are running it, not done with it
        abandonedPlayers.remove(playerId);
        markDirty();

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestPlayerAddedEvent.class, playerId)
                    .dispatch(new QuestPlayerAddedEvent(this, playerId));

        return true;
    }

    /**
     * Removes the player from the quest progression.
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

    /**
     * Moves a player from those running this quest to those who left it. The player will still be linked to the quest
     * but will not receive completion rewards.
     *
     * @return {@code false} if the player was not running this quest.
     */
    public boolean abandonPlayer(@Nonnull UUID playerId) {
        if (!players.remove(playerId)) return false;

        abandonedPlayers.add(playerId);
        markDirty();

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestPlayerAbandonedEvent.class, playerId)
                    .dispatch(new QuestPlayerAbandonedEvent(this, playerId));

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

    /**
     * @return {@code true} if this player gave the quest up.
     */
    public boolean isAbandonedBy(@Nonnull UUID playerId) {
        return abandonedPlayers.contains(playerId);
    }

    /**
     * @return {@code ABANDONED} for players that have left the quest progression and the quest state for others.
     */
    @Nonnull
    public QuestState getStateFor(@Nonnull UUID playerId) {
        return isAbandonedBy(playerId) ? QuestState.ABANDONED : getState();
    }

    /**
     * @return the live, mutable set of ids of the players who gave this quest up.
     */
    @Nonnull
    public Set<UUID> getAbandonedPlayers() {
        return abandonedPlayers;
    }

    /**
     * @return how many players held the quest progression.
     */
    public int getHolderCount() {
        return players.size() + abandonedPlayers.size();
    }

    /**
     * @return when the player was handed this quest, or {@code null} for one they were holding
     * before it was written down.
     */
    @Nullable
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * @return when it reached the outcome it carries, or {@code null} while it is still running.
     */
    @Nullable
    public Instant getCompletedAt() {
        return completedAt;
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
     * @return {@code true} if this quest carries the tag, falling back to its asset when the
     * instance says nothing of it.
     */
    public boolean hasTag(@Nonnull String tag) {
        if (tags.contains(tag)) return true;

        QuestAsset asset = getAsset();
        return asset != null && asset.hasTag(tag);
    }

    /**
     * @return the values its asset writes on a tag, or {@code null} when neither it nor its asset
     * declares it. A tag written on the instance carries no value, so it reads as declared and
     * empty — an instance can silence what an asset named, never rename it.
     */
    @Nullable
    public String[] getTagValues(@Nonnull String tag) {
        QuestAsset asset = getAsset();
        String[] values = asset == null ? null : asset.getTagValues(tag);
        if (values != null) return values;

        return tags.contains(tag) ? NO_TAG_VALUES : null;
    }

    /**
     * @return {@code false} if the quest already carried the tag.
     */
    public boolean addTag(@Nonnull String tag) {
        if (!tags.add(tag)) return false;
        markDirty();

        return true;
    }

    /**
     * @return {@code false} if the quest did not carry the tag.
     */
    public boolean removeTag(@Nonnull String tag) {
        if (!tags.remove(tag)) return false;
        markDirty();

        return true;
    }

    /**
     * @return the live, mutable set of tags written on this quest alone, without those of its asset.
     */
    public Set<String> getTags() {
        return tags;
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

    /**
     * The description of a quest asset, for a quest that no longer has a progression to ask. Empty
     * when the asset names none, since a description is optional.
     */
    @Nonnull
    public static Message descriptionOf(@Nonnull QuestAsset asset) {
        String descriptionKey = asset.getDescriptionKey();

        return descriptionKey != null ? Message.translation(descriptionKey) : asset.create().getDefaultDescription();
    }
}
