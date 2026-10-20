package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAbandonedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Defines the quest progression. Extend it to create new quest types or to add runtime progression data.
 * Avoid using it to declare static serialized data. Look at {@link OpenQuestAsset} for static serialized data declaration.
 */
public abstract class AbstractQuestProgression<Q extends AbstractQuestProgression<Q>> {
    public static final CodecMapCodec<AbstractQuestProgression<?>> CODEC = new CodecMapCodec<>("Type");

    private static final KeyedCodec<UUID[]> PLAYERS_CODEC = new KeyedCodec<>("Players", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));
    private static final KeyedCodec<UUID[]> ABANDONED_PLAYERS_CODEC = new KeyedCodec<>("AbandonedPlayers", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));

    private static final KeyedCodec<Map<String, String[]>> TAGS_CODEC = new KeyedCodec<>("Tags", new MapCodec<>(new ArrayCodec<>(Codec.STRING, String[]::new), HashMap<String, String[]>::new));
    private static final BiConsumer<AbstractQuestProgression, Map<String, String[]>> TAGS_SETTER = (quest, tags) -> ((AbstractQuestProgression<?>) quest).tags.putAll(tags);
    private static final Function<AbstractQuestProgression, Map<String, String[]>> TAGS_GETTER = (quest) -> ((AbstractQuestProgression<?>) quest).tags;

    public static final String[] NO_TAG_VALUES = new String[0];

    /**
     * Serializes the fields shared by every quest progression; concrete codecs chain from this.
     */
    public static final BuilderCodec<AbstractQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(AbstractQuestProgression.class)
                                                                                        .append(new KeyedCodec<>("Id", Codec.UUID_STRING), (quest, uuid) -> quest.id = uuid, quest -> quest.id)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("AssetId", Codec.STRING), (quest, assetId) -> quest.assetId = assetId, quest -> quest.assetId)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("ParentId", Codec.UUID_STRING), (quest, parentId) -> quest.parentId = parentId, quest -> quest.parentId)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("State", new EnumCodec<>(QuestState.class)), (quest, state) -> quest.stateVersion = new QuestStateVersion(state, quest.stateVersion.version()), quest -> quest.stateVersion.state())
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("Transitions", Codec.INTEGER), (quest, count) -> quest.stateVersion = new QuestStateVersion(quest.stateVersion.state(), count), quest -> Integer.valueOf(quest.stateVersion.version()))
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("PersistHistory", Codec.BOOLEAN), (quest, value) -> quest.persistHistory = value, quest -> quest.persistHistory)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("Visibility", new EnumCodec<>(QuestVisibility.class)), (quest, value) -> quest.visibility = value, quest -> quest.visibility)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("AnnounceOutcome", Codec.BOOLEAN), (quest, value) -> quest.announceOutcome = value, quest -> quest.announceOutcome)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("StartedAt", Codec.LONG), (quest, millis) -> quest.startedAt = Instant.ofEpochMilli(millis), quest -> quest.startedAt == null ? null : Long.valueOf(quest.startedAt.toEpochMilli()))
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("CompletedAt", Codec.LONG), (quest, millis) -> quest.completedAt = Instant.ofEpochMilli(millis), quest -> quest.completedAt == null ? null : Long.valueOf(quest.completedAt.toEpochMilli()))
                                                                                        .add()
                                                                                        .append(PLAYERS_CODEC, (quest, ids) -> ((AbstractQuestProgression<?>) quest).players.addAll(List.of(ids)), quest -> ((AbstractQuestProgression<?>) quest).players.toArray(UUID[]::new))
                                                                                        .add()
                                                                                        .append(ABANDONED_PLAYERS_CODEC, (quest, ids) -> ((AbstractQuestProgression<?>) quest).abandonedPlayers.addAll(List.of(ids)), quest -> ((AbstractQuestProgression<?>) quest).abandonedPlayers.toArray(UUID[]::new))
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("Scope", QuestScope.CODEC), (quest, scope) -> quest.scope = scope, quest -> quest.scope)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("Origin", QuestOrigin.CODEC), (quest, origin) -> quest.origin = origin, quest -> quest.origin)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("GrantedBy", Codec.UUID_STRING), (quest, grantedBy) -> quest.grantedBy = grantedBy, quest -> quest.grantedBy)
                                                                                        .add()
                                                                                        .append(TAGS_CODEC, TAGS_SETTER, TAGS_GETTER)
                                                                                        .add()
                                                                                        .build();

    /**
     * Unique identity used to reference and persist this quest. Overwritten on load.
     */
    private UUID id = UUID.randomUUID();

    /**
     * Ids of the players running the quest.
     */
    private Set<UUID> players = ConcurrentHashMap.newKeySet();

    /**
     * Ids of the players who gave it up, never also running it.
     */
    private Set<UUID> abandonedPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Who shares this quest beyond its players, written by the scope holding it. Data only: the
     * quest does nothing with it. {@code null} for a quest its players hold on their own.
     */
    @Nullable
    private QuestScope scope;

    /**
     * Which assignment handed this quest out and on which occasion, carried down a chain.
     * {@code null} for a quest no assignment handed out.
     */
    @Nullable
    private QuestOrigin origin;

    /**
     * The quest whose completion handed this one over, {@code null} for one nothing handed over.
     * Tells apart two runs of one chain, which share every asset.
     */
    @Nullable
    private UUID grantedBy;

    /**
     * Tags written on this instance, on top of those its asset declares, each with the values its
     * asset counterpart would carry. What became of this one quest belongs here, since an asset is
     * shared by everyone holding it.
     */
    private Map<String, String[]> tags = new ConcurrentHashMap<>();

    /**
     * The {@link OpenQuestAsset#getId()}
     */
    private String assetId;

    /**
     * The composite this quest is a step of, {@code null} for a quest standing on its own. Only a
     * composite writes it, as it adopts the step, which is what keeps children to composites.
     */
    @Nullable
    private UUID parentId;

    /**
     * Written with the quest: the version tells an older outcome from a newer one, for a group
     * keeping how its steps went.
     */
    @Nonnull
    private QuestStateVersion stateVersion = QuestStateVersion.INITIAL;

    /**
     * What this copy knows of the stored copy it stands on, for a quest other servers write too.
     */
    @Nonnull
    private final transient QuestReplica<Q> replica = new QuestReplica<>(self());

    /**
     * Grows with every change, so that one update can tell whether it changed anything.
     */
    private transient int changeCount;

    /**
     * Above zero while an operation is being made, where marking the quest dirty is expected.
     */
    private transient int applying;

    /**
     * Overrides the asset for this instance alone. Boxed so that "not overridden" is a state of
     * its own, and so the codec leaves it out entirely.
     */
    @Nullable
    private Boolean persistHistory;

    /**
     * Overrides the asset on when this one run is worth listing, { null} while the asset
     * answers for it.
     */
    @Nullable
    private QuestVisibility visibility;

    /**
     * Overrides the asset on whether this one run announces how it ended. Set on a step by the
     * chain that handed it out, the way {@code PersistHistory} is.
     */
    @Nullable
    private Boolean announceOutcome;

    /**
     * When the player was handed this quest, written once as it enters the store. Null for a quest
     * registered before this was kept, which is a date nobody can invent after the fact.
     */
    @Nullable
    private Instant startedAt;

    /**
     * When it reached the outcome it now carries. Rewritten whenever that outcome changes, and
     * struck out if the quest goes back to running, as a quest kept alive by {@code StopOnComplete:
     * false} can, and a date left standing would say it ended when it did not.
     */
    @Nullable
    private Instant completedAt;

    /**
     * Set when the runtime state changed; drives incremental disk saves. Not serialized.
     */
    transient boolean dirty;

    /**
     * What the quest acts through, attached by the store holding it. A quest no store holds yet
     * tells nobody of its changes and is written by this server alone.
     */
    @Nullable
    private transient QuestEnvironment environment;

    /**
     * @return whether reaching a terminal state ends this quest. One that says {@code false} keeps
     * running and being re-evaluated, so its state can still change.
     */
    public boolean isStopOnComplete() {
        OpenQuestAsset asset = getAsset();
        return asset == null || asset.isStopOnComplete();
    }

    /**
     * @return whether completing this quest is recorded in its players history. A quest handed out
     * as part of another one can be told not to, whatever its asset says.
     */
    public boolean isPersistHistory() {
        if (persistHistory != null) return persistHistory;

        OpenQuestAsset asset = getAsset();
        return asset == null || asset.isPersistHistory();
    }

    /**
     * @return whether a player may give this quest up. Falls back to allowing it when the asset
     * is gone, so a quest nobody can name is not also one nobody can be rid of.
     */
    public boolean canBeAbandoned() {
        OpenQuestAsset asset = getAsset();

        return asset == null || asset.canBeAbandoned();
    }

    /**
     * @return when this quest is worth putting in front of the player, its asset answering while
     * the quest says nothing itself.
     */
    @Nonnull
    public QuestVisibility getVisibility() {
        if (visibility != null) return visibility;

        OpenQuestAsset asset = getAsset();
        return asset == null ? QuestVisibility.ALWAYS : asset.getVisibility();
    }

    /**
     * @return whether the player has got anywhere with this quest. A type that cannot be partway
     * through has only the two answers, so it says so here; a counted one overrides.
     */
    public boolean hasProgressed() {
        return isCompleted();
    }

    /**
     * @return whether the quest is worth listing right now, which is what every panel and page
     * drawing quests asks before drawing one. What it owes the player is untouched by the answer.
     */
    public boolean isVisible() {
        return getVisibility().isReachedBy(this);
    }

    /**
     * @return whether this quest says how it ended. Nothing to do with where the quest is listed:
     * one is the moment it ends, the other the lists it belongs on.
     */
    public boolean isAnnounceOutcome() {
        if (announceOutcome != null) return announceOutcome;

        OpenQuestAsset asset = getAsset();
        return asset == null || asset.isAnnounceOutcome();
    }

    /**
     * Has the visitor say what happened to the quest, then gives up a quest nobody runs any more.
     * A player's action is first put to the constraints of the asset, and leaves no trace if one
     * objects. A quest that is over takes no more.
     *
     * @param visitor the visitor to apply
     */
    public void update(QuestVisitor<Q> visitor) {
        UUID actorId = visitor.getActorId();
        if (actorId != null && !allowsProgress(actorId)) return;
        if (isOver()) return;

        int before = changeCount;
        visitor.progress(self());

        // If no player remains and some has abandoned set the quest as abandoned
        if (!isCompleted() && players.isEmpty() && !abandonedPlayers.isEmpty()) apply(new AbstractQuestProgression.SetState(QuestState.ABANDONED));

        if (changeCount != before) announceUpdate();
    }

    /**
     * Makes an operation on the quest, the one way anything about it changes. A quest other
     * servers write too keeps it, to make it again over whatever they stored meanwhile, and hears
     * of a change of state once the write making it for all of them is done.
     *
     * @return whether it changed anything, the quest then waiting to be written
     */
    public boolean apply(@Nonnull QuestOperation<? super Q> operation) {
        QuestState previousState;
        QuestStateVersion now;
        boolean kept;
        synchronized (this) {
            previousState = getState();
            boolean unsaved = dirty;

            if (!make(operation)) return false;

            now = stateVersion;
            kept = replica.keep(operation, unsaved);
        }

        if (now.state() == previousState) return true;

        if (kept) {
            environment().writeSoon(this);
        } else {
            announce(previousState, now, true);
        }
        return true;
    }

    /**
     * Makes the operation and dates a change of state, telling nobody.
     */
    boolean make(@Nonnull QuestOperation<? super Q> operation) {
        QuestState previousState = getState();

        applying++;
        try {
            if (!operation.applyTo(self())) return false;

            if (getState() != previousState) completedAt = isCompleted() ? now() : null;
            markDirty();
            return true;
        } finally {
            applying--;
        }
    }

    /**
     * Tells everyone here the quest changed state: as it changes, for a quest only this server
     * writes; once written, for one other servers write too, by the write that made the change or
     * brought news of it.
     *
     * @param now where the quest stands after the change, at the version this change made
     * @param madeHere whether this server made the change, and pays what an end pays
     */
    public void announce(@Nonnull QuestState previousState, @Nonnull QuestStateVersion now, boolean madeHere) {
        if (now.state().isCompleted()) {
            dispatch(QuestCompletedEvent.class, getId(), new QuestCompletedEvent(this, now.state(), madeHere));
        }

        // Last, so a listener reacting to the transition reads a quest that has already been paid
        // and filed rather than one still being settled.
        dispatch(QuestStateChangedEvent.class, getId(), new QuestStateChangedEvent(this, previousState, now));
    }

    /**
     * Tells whatever draws the quest that it moved.
     */
    public void announceUpdate() {
        dispatch(QuestUpdatedEvent.class, getId(), new QuestUpdatedEvent(this));
    }

    private <E extends IEvent<UUID>> void dispatch(@Nonnull Class<E> type, @Nonnull UUID key, @Nonnull E event) {
        if (environment != null) environment.events().dispatchFor(type, key).dispatch(event);
    }

    /**
     * Gives the quest what it acts through. Called by the store as the quest enters it or is read
     * back into it.
     */
    public void attach(@Nonnull QuestEnvironment environment) {
        this.environment = environment;
    }

    /**
     * @return what the quest acts through.
     * @throws IllegalStateException for a quest no store holds yet
     */
    @Nonnull
    public QuestEnvironment environment() {
        if (environment == null) throw new IllegalStateException("Quest " + id + " is not held by any store yet");
        return environment;
    }

    /**
     * The environment's time, the system's for a quest no store holds yet.
     */
    @Nonnull
    private Instant now() {
        return environment != null ? environment.clock().instant() : Instant.now();
    }

    /**
     * @return whether other servers may write this quest too, which only a stored quest can be.
     */
    public boolean isShared() {
        return environment != null && environment.isShared(this);
    }

    /**
     * @return what this copy knows of the stored copy it stands on, for the storage and the sync.
     */
    @Nonnull
    public QuestReplica<Q> getReplica() {
        return replica;
    }

    /**
     * @return where the quest stands, with the version ordering it against older news of it.
     */
    @Nonnull
    public QuestStateVersion getStateVersion() {
        return stateVersion;
    }

    /**
     * @return whether what that player did may count towards this quest, every constraint of its
     * asset agreeing, and those of every group it is a step of: a step is played inside its group.
     * An asset that is gone holds nothing back.
     */
    public boolean allowsProgress(@Nonnull UUID actorId) {
        OpenQuestAsset asset = getAsset();
        if (asset != null) {
            for (QuestConstraint constraint : asset.getConstraints()) {
                if (!constraint.allowsProgress(this, actorId)) return false;
            }
        }

        AbstractCompositeQuestProgression<?> parent = getParent();
        return parent == null || parent.allowsProgress(actorId);
    }

    /**
     * Called just after the quest progression entered the quest store.
     * Called by {@link QuestProgressionService#registerQuest(AbstractQuestProgression)}.
     *
     * <p>Runs once, when the quest is first handed out, and not when one is read back from disk,
     * which is what makes it the moment the quest started.
     *
     * @param startedAt the moment it was handed out, read off the services' clock
     */
    public void onRegistered(@Nonnull Instant startedAt) {
        this.startedAt = startedAt;
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
        if (!apply(new AbstractQuestProgression.Join(playerId))) return false;

        dispatch(QuestPlayerAddedEvent.class, playerId, new QuestPlayerAddedEvent(this, playerId));

        return true;
    }

    /**
     * Removes the player from the quest progression.
     * @return {@code false} if the player did not hold this quest.
     */
    public boolean removePlayer(@Nonnull UUID playerId) {
        if (!apply(new AbstractQuestProgression.Leave(playerId))) return false;

        dispatch(QuestPlayerRemovedEvent.class, playerId, new QuestPlayerRemovedEvent(this, playerId));

        return true;
    }

    /**
     * Moves a player from those running this quest to those who left it. The player will still be linked to the quest
     * but will not receive completion rewards.
     *
     * @return {@code false} if the player was not running this quest.
     */
    public boolean abandonPlayer(@Nonnull UUID playerId) {
        if (!apply(new AbstractQuestProgression.Abandon(playerId))) return false;

        dispatch(QuestPlayerAbandonedEvent.class, playerId, new QuestPlayerAbandonedEvent(this, playerId));

        return true;
    }

    public boolean isSuccessful() {
        return getState() == QuestState.SUCCESSFUL;
    }

    public boolean isFailed() {
        return getState() == QuestState.FAILED;
    }

    public boolean isAbandoned() {
        return getState() == QuestState.ABANDONED;
    }

    public boolean isCompleted() {
        return getState().isCompleted();
    }

    /**
     * @return whether the quest stopped for good: ended, and not kept running by {@code
     * StopOnComplete: false}, whose state can still change. Nothing moves a quest that is over.
     */
    public boolean isOver() {
        return isCompleted() && isStopOnComplete();
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
     * @return the ids of the players who gave this quest up, read only: moves go through
     * {@link #abandonPlayer}.
     */
    @Nonnull
    public Set<UUID> getAbandonedPlayers() {
        return Collections.unmodifiableSet(abandonedPlayers);
    }

    /**
     * @return who shares this quest beyond its players, {@code null} for one they hold on their own.
     */
    @Nullable
    public QuestScope getScope() {
        return scope;
    }

    /**
     * @return which assignment handed this quest out and on which occasion, {@code null} for one
     * no assignment handed out.
     */
    @Nullable
    public QuestOrigin getOrigin() {
        return origin;
    }

    /**
     * @return the quest whose completion handed this one over, {@code null} for one nothing
     * handed over.
     */
    @Nullable
    public UUID getGrantedBy() {
        return grantedBy;
    }

    /**
     * @return how many players held the quest progression.
     */
    public int getHolderCount() {
        return getPlayers().size() + getAbandonedPlayers().size();
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

    public OpenQuestAsset getAsset() {
        return OpenQuestAsset.getAsset(assetId);
    }

    public UUID getId() {
        return id;
    }

    public String getAssetId() {
        return assetId;
    }

    /**
     * @return the id of the composite this quest is a step of, {@code null} for one standing on
     * its own. Answers even once the composite has left memory.
     */
    @Nullable
    public UUID getParentId() {
        return parentId;
    }

    /**
     * @return the composite this quest is a step of, {@code null} for one standing on its own or
     * whose composite is not in memory. Never reads anything back.
     */
    @Nullable
    public AbstractCompositeQuestProgression<?> getParent() {
        if (parentId == null) return null;

        if (environment == null) return null;

        return environment.services().getQuestProgressionService().getQuest(parentId) instanceof AbstractCompositeQuestProgression<?> parent ? parent : null;
    }

    /**
     * @return the outermost group this quest is a step of, or the quest itself: a step carries no
     * scope of its own and is shared the way its head is. Never reads anything back.
     */
    @Nonnull
    public AbstractQuestProgression<?> getHead() {
        AbstractQuestProgression<?> head = this;
        for (AbstractCompositeQuestProgression<?> parent = getParent(); parent != null; parent = parent.getParent()) {
            head = parent;
        }
        return head;
    }

    /**
     * Written by {@link AbstractCompositeQuestProgression} alone, as it adopts a step.
     */
    void setParentId(@Nullable UUID parentId) {
        this.parentId = parentId;
    }

    /**
     * @return {@code true} if this quest carries the tag, falling back to its asset when the
     * instance says nothing of it.
     */
    public boolean hasTag(@Nonnull String tag) {
        if (tags.containsKey(tag)) return true;

        OpenQuestAsset asset = getAsset();
        return asset != null && asset.hasTag(tag);
    }

    /**
     * The instance is asked first and answers alone once it declares the tag, the same override as
     * {@code PersistHistory}: a quest written over by hand is not arguing with its template, it is
     * replacing what the template said.
     *
     * @return the values written on a tag, empty when it is declared without any, and {@code null}
     * when neither the quest nor its asset declares it at all.
     */
    @Nullable
    public String[] getTagValues(@Nonnull String tag) {
        String[] own = tags.get(tag);
        if (own != null) return own;

        OpenQuestAsset asset = getAsset();
        return asset == null ? null : asset.getTagValues(tag);
    }

    /**
     * @return the tags written on this quest alone, without those of its asset, read only: writes
     * are {@link PutTag} and {@link RemoveTag} operations.
     */
    @Nonnull
    public Map<String, String[]> getTags() {
        return Collections.unmodifiableMap(tags);
    }

    /**
     * @return the ids of the players this quest is assigned to, read only: moves go through
     * {@link #addPlayer} and {@link #removePlayer}.
     */
    @Nonnull
    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    @Nonnull
    public QuestState getState() {
        return stateVersion.state();
    }

    /**
     * Ends the quest on that state, or brings one kept running by {@code StopOnComplete: false}
     * back. A quest that is over takes no more. An operation of a subtype settling the quest makes
     * this one within its own.
     */
    public record SetState(@Nonnull QuestState state) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            if (quest.isOver() || quest.getState() == state) return false;

            quest.stateVersion = quest.stateVersion.with(state);
            return true;
        }
    }

    /**
     * Hands the quest to a player, one who walked away from it included: they run it again.
     */
    public record Join(@Nonnull UUID playerId) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            if (!quest.players.add(playerId)) return false;

            quest.abandonedPlayers.remove(playerId);
            return true;
        }
    }

    /**
     * Takes a player off the quest without them giving it up, as a world left behind does.
     */
    public record Leave(@Nonnull UUID playerId) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.players.remove(playerId);
        }
    }

    /**
     * A player running the quest gives it up.
     */
    public record Abandon(@Nonnull UUID playerId) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            if (!quest.players.remove(playerId)) return false;

            quest.abandonedPlayers.add(playerId);
            return true;
        }
    }

    /**
     * Writes a tag and what it carries, replacing whatever the quest held under it.
     */
    public record PutTag(@Nonnull String tag, @Nonnull String[] values) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return !Arrays.equals(quest.tags.put(tag, values), values);
        }

        /**
         * Declares a tag carrying nothing.
         */
        public PutTag(@Nonnull String tag) {
            this(tag, NO_TAG_VALUES);
        }
    }

    public record RemoveTag(@Nonnull String tag) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.tags.remove(tag) != null;
        }
    }

    /**
     * Written by the scope holding the quest, never by the quest itself.
     */
    public record SetScope(@Nullable QuestScope scope) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            if (quest.scope == scope) return false;

            quest.scope = scope;
            return true;
        }
    }

    /**
     * The quest reached one more world of the scope holding it.
     */
    public record AddWorld(@Nonnull UUID worldId) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.scope != null && quest.scope.addWorld(worldId);
        }
    }

    /**
     * A world of the scope holding the quest closed.
     */
    public record RemoveWorld(@Nonnull UUID worldId) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.scope != null && quest.scope.removeWorld(worldId);
        }
    }

    /**
     * @return the title of the asset, or what the type makes of itself when the asset names none.
     */
    @Nonnull
    public Message getTitle() {
        OpenQuestAsset asset = getAsset();
        String titleKey = asset == null ? null : asset.getTitleKey();

        return titleKey != null ? Message.translation(titleKey) : getDefaultTitle();
    }

    @Nonnull
    public Message getDescription() {
        OpenQuestAsset asset = getAsset();
        String descriptionKey = asset == null ? null : asset.getDescriptionKey();

        return descriptionKey != null ? Message.translation(descriptionKey) : getDefaultDescription();
    }

    /**
     * @return the line saying concretely what to do, what the HUD shows. Empty when the asset names
     * none: a title saying it already would only be repeated.
     */
    @Nonnull
    public Message getShortDescription() {
        OpenQuestAsset asset = getAsset();
        return asset == null ? Message.raw("") : shortDescriptionOf(asset);
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
     * Marks this quest as needing to be persisted on the next save pass. A quest other servers
     * write too changes through {@link #apply}: a change made past it is reported, and only ever
     * written as a copy of the whole quest.
     */
    public void markDirty() {
        this.dirty = true;
        changeCount++;

        if (applying == 0) replica.reportUnkept();
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
     * Sets a quest up before anyone holds it. What it is given here is its configuration, fixed
     * once built: every later change is a {@link QuestOperation} made through {@link #apply}.
     *
     * @param <Q> the quest being built
     * @param <B> the concrete builder, so that chained calls keep its own methods
     */
    public abstract static class Builder<Q extends AbstractQuestProgression<Q>, B extends Builder<Q, B>> {
        @Nullable
        private Q quest;

        protected Builder(@Nonnull Q quest, @Nonnull String assetId) {
            ((AbstractQuestProgression<?>) quest).assetId = Objects.requireNonNull(assetId, "assetId");
            this.quest = quest;
        }

        /**
         * @return the quest under construction, for a subtype's builder to configure.
         * @throws IllegalStateException once the quest is built and handed over
         */
        @Nonnull
        protected Q quest() {
            if (quest == null) throw new IllegalStateException("This builder already handed its quest over");
            return quest;
        }

        /**
         * Overrides the asset on whether completing this quest is recorded in its players history.
         */
        public B persistHistory(@Nullable Boolean persistHistory) {
            base().persistHistory = persistHistory;
            return self();
        }

        /**
         * Overrides the asset on when this quest is worth listing.
         */
        public B visibility(@Nullable QuestVisibility visibility) {
            base().visibility = visibility;
            return self();
        }

        /**
         * Overrides the asset on whether this quest announces how it ended.
         */
        public B announceOutcome(@Nullable Boolean announceOutcome) {
            base().announceOutcome = announceOutcome;
            return self();
        }

        /**
         * The assignment and occasion handing this quest out, carried down a chain.
         */
        public B origin(@Nullable QuestOrigin origin) {
            base().origin = origin;
            return self();
        }

        /**
         * The quest whose completion hands this one over.
         */
        public B grantedBy(@Nullable UUID grantedBy) {
            base().grantedBy = grantedBy;
            return self();
        }

        /**
         * Hands the quest over, after which this builder refuses everything.
         *
         * @throws IllegalStateException for an override no quest could run with
         */
        @Nonnull
        public Q build() {
            Q built = quest();
            validate(built);
            quest = null;
            return built;
        }

        /**
         * Refuses an override no quest could run with, each subtype checking its own.
         *
         * @throws IllegalStateException naming what is wrong
         */
        protected void validate(@Nonnull Q quest) {}

        @SuppressWarnings("unchecked")
        protected B self() {
            return (B) this;
        }

        @Nonnull
        private AbstractQuestProgression<?> base() {
            return quest();
        }

        /**
         * @return a builder for a quest type with nothing of its own to override.
         */
        @Nonnull
        public static <Q extends AbstractQuestProgression<Q>> Builder<Q, ?> of(@Nonnull Q quest, @Nonnull String assetId) {
            return new PlainBuilder<>(quest, assetId);
        }
    }

    private static final class PlainBuilder<Q extends AbstractQuestProgression<Q>> extends Builder<Q, PlainBuilder<Q>> {
        private PlainBuilder(@Nonnull Q quest, @Nonnull String assetId) {
            super(quest, assetId);
        }
    }

    /**
     * The title of a quest asset. If the asset has no title, the quest is instantiated to read the default one.
     */
    @Nonnull
    public static Message titleOf(@Nonnull OpenQuestAsset asset) {
        String titleKey = asset.getTitleKey();

        return titleKey != null ? Message.translation(titleKey) : asset.create().getDefaultTitle();
    }

    /**
     * The description of a quest asset, for a quest that no longer has a progression to ask. Empty
     * when the asset names none, since a description is optional.
     */
    @Nonnull
    public static Message descriptionOf(@Nonnull OpenQuestAsset asset) {
        String descriptionKey = asset.getDescriptionKey();

        return descriptionKey != null ? Message.translation(descriptionKey) : asset.create().getDefaultDescription();
    }

    /**
     * The short description of a quest asset, for a quest that no longer has a progression to ask.
     */
    @Nonnull
    public static Message shortDescriptionOf(@Nonnull OpenQuestAsset asset) {
        String key = asset.getShortDescriptionKey();

        return key != null ? Message.translation(key) : Message.raw("");
    }
}
