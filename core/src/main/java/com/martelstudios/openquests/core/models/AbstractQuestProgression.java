package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAbandonedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.replication.Membership;
import com.martelstudios.openquests.core.replication.QuestTransitions;
import com.martelstudios.openquests.core.replication.StoredState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Defines the quest progression. Extend it to create new quest types or to add runtime progression data.
 * Avoid using it to declare static serialized data. Look at {@link OpenQuestAsset} for static serialized data declaration.
 */
public abstract class AbstractQuestProgression<Q extends AbstractQuestProgression<Q>> {
    public static final CodecMapCodec<AbstractQuestProgression<?>> CODEC = new CodecMapCodec<>("Type");

    /**
     * Players written as plain lists before moves were kept, read back but never written: whatever
     * a copy with moves says of them wins.
     */
    private static final KeyedCodec<UUID[]> PLAIN_PLAYERS_CODEC = new KeyedCodec<>("Players", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));
    private static final KeyedCodec<UUID[]> PLAIN_ABANDONED_CODEC = new KeyedCodec<>("AbandonedPlayers", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));

    private static final KeyedCodec<Map<String, String[]>> TAGS_CODEC = new KeyedCodec<>("Tags", new MapCodec<>(new ArrayCodec<>(Codec.STRING, String[]::new), HashMap<String, String[]>::new));
    private static final BiConsumer<AbstractQuestProgression, Map<String, String[]>> TAGS_SETTER = (quest, tags) -> ((AbstractQuestProgression<?>) quest).tags.putAll(tags);
    private static final Function<AbstractQuestProgression, Map<String, String[]>> TAGS_GETTER = (quest) -> ((AbstractQuestProgression<?>) quest).tags;

    public static final String[] NO_TAG_VALUES = new String[0];

    /**
     * The asset class each quest type casts its asset to, recorded as the types are registered, so
     * a quest read back can be checked against whatever stands under its asset id today.
     */
    private static final Map<Class<?>, Class<? extends OpenQuestAsset>> ASSET_CLASSES = new ConcurrentHashMap<>();

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
                                                                                        .append(new KeyedCodec<>("State", new EnumCodec<>(QuestState.class)), AbstractQuestProgression::restoreState, quest -> quest.state)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("PersistHistory", Codec.BOOLEAN), (quest, value) -> quest.persistHistory = value, quest -> quest.persistHistory)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("Track", Codec.BOOLEAN), (quest, value) -> quest.track = value, quest -> quest.track)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("Visibility", new EnumCodec<>(QuestVisibility.class)), (quest, value) -> quest.visibility = value, quest -> quest.visibility)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("AnnounceOutcome", Codec.BOOLEAN), (quest, value) -> quest.announceOutcome = value, quest -> quest.announceOutcome)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("StartedAt", Codec.LONG), (quest, millis) -> quest.startedAt = Instant.ofEpochMilli(millis), quest -> quest.startedAt == null ? null : Long.valueOf(quest.startedAt.toEpochMilli()))
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("CompletedAt", Codec.LONG), (quest, millis) -> quest.completedAt = Instant.ofEpochMilli(millis), quest -> quest.completedAt == null ? null : Long.valueOf(quest.completedAt.toEpochMilli()))
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("Membership", Membership.CODEC), (quest, moves) -> quest.membership.putAll(moves), quest -> quest.membership.toMap())
                                                                                        .add()
                                                                                        .append(PLAIN_PLAYERS_CODEC, (quest, ids) -> quest.membership.putPlain(List.of(ids), List.of()), quest -> null)
                                                                                        .add()
                                                                                        .append(PLAIN_ABANDONED_CODEC, (quest, ids) -> quest.membership.putPlain(List.of(), List.of(ids)), quest -> null)
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
    protected UUID id = UUID.randomUUID();

    /**
     * Who runs the quest and who gave it up, merged move by move with what other servers saw.
     */
    protected final Membership membership = new Membership();

    /**
     * Who shares this quest beyond its players, written by the scope holding it. Data only: the
     * quest does nothing with it. {@code null} for a quest its players hold on their own.
     */
    @Nullable
    protected QuestScope scope;

    /**
     * Which assignment handed this quest out and on which occasion, carried down a chain.
     * {@code null} for a quest no assignment handed out.
     */
    @Nullable
    protected QuestOrigin origin;

    /**
     * The quest whose completion handed this one over, {@code null} for one nothing handed over.
     * Tells apart two runs of one chain, which share every asset.
     */
    @Nullable
    protected UUID grantedBy;

    /**
     * The outcome everyone here last heard of, so that an end reached here and the same end learnt
     * from another server are announced once.
     */
    private final AtomicReference<QuestState> announced = new AtomicReference<>(QuestState.IN_PROGRESS);

    /**
     * Tags written on this instance, on top of those its asset declares, each with the values its
     * asset counterpart would carry. What became of this one quest belongs here, since an asset is
     * shared by everyone holding it.
     */
    protected Map<String, String[]> tags = new ConcurrentHashMap<>();

    /**
     * The {@link OpenQuestAsset#getId()}
     */
    protected String assetId;

    /**
     * The composite this quest is a step of, {@code null} for a quest standing on its own. Only a
     * composite writes it, as it adopts the step, which is what keeps children to composites.
     */
    @Nullable
    private UUID parentId;

    protected QuestState state = QuestState.IN_PROGRESS;

    /**
     * How many changes of state this copy went through, as the storage counts them: a change is
     * only written over the epoch it started from, and a copy only takes in a later one. Never
     * written with the quest: the storage keeps it next to the state.
     */
    private volatile long stateEpoch;

    /**
     * Overrides the asset for this instance alone. Boxed so that "not overridden" is a state of
     * its own, and so the codec leaves it out entirely.
     */
    @Nullable
    protected Boolean persistHistory;

    /**
     * Whether the player is tracking this quest, {@code null} while they have not said either way
     * and the asset still answers for it.
     */
    @Nullable
    protected Boolean track;

    /**
     * Overrides the asset on when this one run is worth listing, { null} while the asset
     * answers for it.
     */
    @Nullable
    protected QuestVisibility visibility;

    /**
     * Overrides the asset on whether this one run announces how it ended. Set on a step by the
     * chain that handed it out, the way {@code PersistHistory} is.
     */
    @Nullable
    protected Boolean announceOutcome;

    /**
     * When the player was handed this quest, written once as it enters the store. Null for a quest
     * registered before this was kept, which is a date nobody can invent after the fact.
     */
    @Nullable
    protected Instant startedAt;

    /**
     * When it reached the outcome it now carries. Rewritten whenever that outcome changes, and
     * struck out if the quest goes back to running, as a quest kept alive by {@code StopOnComplete:
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

    public Q setPersistHistory(@Nullable Boolean persistHistory) {
        this.persistHistory = persistHistory;
        return self();
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

    public Q setVisibility(@Nullable QuestVisibility visibility) {
        this.visibility = visibility;
        return self();
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
        return switch (getVisibility()) {
            case ALWAYS -> true;
            case WHEN_PROGRESSED -> hasProgressed();
            case WHEN_COMPLETED -> isCompleted();
            case NEVER -> false;
        };
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

    public Q setAnnounceOutcome(@Nullable Boolean announceOutcome) {
        this.announceOutcome = announceOutcome;
        return self();
    }

    /**
     * @return what the quest says about being tracked, its asset answering while it says nothing
     * itself. What follows from it is left to whoever draws the quest somewhere.
     */
    public boolean isTracked() {
        if (track != null) return track;

        OpenQuestAsset asset = getAsset();
        return asset != null && asset.isAutoTrack();
    }

    /**
     * @param track {@code null} to hand the answer back to the asset, which is not the same as
     * saying no: the quest stops having an opinion of its own.
     * @return {@code false} if the quest already said exactly that.
     */
    public boolean setTracked(@Nullable Boolean track) {
        if (Objects.equals(this.track, track)) return false;

        this.track = track;
        markDirty();

        return true;
    }

    /**
     * Takes in what another copy of this quest knows, read from another server's share: who holds
     * it and how far it went. Never marks this copy dirty, what is merged being stored already. Where
     * it stands is not taken from a copy: each change of state is claimed, and heard through
     * {@link #adoptStoredState}.
     *
     * @return whether anything the players see moved.
     */
    public final boolean merge(@Nonnull AbstractQuestProgression<?> other) {
        if (other == this || other.getClass() != getClass() || !other.getId().equals(getId())) return false;

        @SuppressWarnings("unchecked")
        Q same = (Q) other;

        boolean progressed = membership.merge(other.membership);
        return mergeProgress(same) | progressed;
    }

    /**
     * @return how many changes of state this copy went through, as the storage counts them.
     */
    public long getStateEpoch() {
        return stateEpoch;
    }

    /**
     * Takes in where the storage says the quest stands, as it is read back: that is where it
     * stands, whatever the replica this copy was built from says. Fires nothing, nothing having
     * changed for anyone: an outcome read back was announced by whichever server reached it.
     */
    public void restoreStoredState(@Nonnull StoredState stored) {
        state = stored.state();
        completedAt = stored.at();
        stateEpoch = stored.epoch();
        announced.set(stored.state());
    }

    /**
     * Takes in a change of state another server claimed, which this one learns afterwards:
     * everything here hears of it, to file the quest and tell its players, and nothing is paid
     * twice. A change no later than what this copy holds already is no news.
     *
     * @return whether this copy's state changed.
     */
    public boolean adoptStoredState(@Nonnull StoredState stored) {
        if (stored.epoch() <= stateEpoch) return false;

        QuestState previous = state;
        state = stored.state();
        completedAt = stored.at();
        stateEpoch = stored.epoch();
        if (state == previous) return false;

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestUpdatedEvent.class, getId())
                    .dispatch(new QuestUpdatedEvent(this));
        if (isCompleted()) announceEnd(false);
        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestStateChangedEvent.class, getId())
                    .dispatch(new QuestStateChangedEvent(this, previous));
        return true;
    }

    /**
     * Tells everyone here the quest ended, once per outcome whichever way this server learnt of
     * it: reaching the end itself, or hearing another server claimed it.
     *
     * @param claimedHere whether this server ended it, and pays what the end pays
     */
    private void announceEnd(boolean claimedHere) {
        QuestState outcome = state;
        if (announced.getAndSet(outcome) == outcome) return;

        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestCompletedEvent.class, getId())
                    .dispatch(new QuestCompletedEvent(this, claimedHere));
    }

    /**
     * An outcome read back has already been announced, by whichever server reached it.
     */
    private void restoreState(@Nonnull QuestState restored) {
        state = restored;
        announced.set(restored);
    }

    /**
     * Takes in the progress another copy of the same quest made. A type whose progress several
     * servers move overrides this; what it keeps for its own server alone, it leaves be.
     *
     * @return whether anything moved.
     */
    protected boolean mergeProgress(@Nonnull Q other) {
        return false;
    }

    /**
     * Updates the quest progression by applying the visitor to it.
     * After the visitor's pass, the quest settles before anyone hears about it.
     * A player's action is first put to the constraints of the asset, and leaves no trace if one objects.
     *
     * @param visitor the visitor to apply
     */
    public void update(QuestVisitor<Q> visitor) {
        UUID actorId = visitor.getActorId();
        if (actorId != null && !allowsProgress(actorId)) return;

        QuestState previousState = getState();

        visitor.progress(self());

        // If no player remains and some has abandoned set the quest as abandoned
        if (!isCompleted() && getPlayers().isEmpty() && !getAbandonedPlayers().isEmpty()) {
            setState(QuestState.ABANDONED).markDirty();
        }

        if (hasChanges()) {
            HytaleServer.get()
                        .getEventBus()
                        .dispatchFor(QuestUpdatedEvent.class, getId())
                        .dispatch(new QuestUpdatedEvent(this));
        }

        // Every change of state is claimed, an end as much as a quest kept alive by StopOnComplete:false
        // going back to running or changing its outcome: servers holding the quest agree on each in
        // turn, and a copy behind takes in where the quest stands rather than its own view.
        if (getState() != previousState) {
            completedAt = isCompleted() ? Instant.now() : null;
            markDirty();

            StoredState claimedElsewhere = QuestTransitions.claim(this);
            boolean claimedHere = claimedElsewhere == null;
            if (claimedHere) {
                stateEpoch++;
            } else {
                state = claimedElsewhere.state();
                completedAt = claimedElsewhere.at();
                stateEpoch = claimedElsewhere.epoch();
            }

            if (isCompleted()) announceEnd(claimedHere);
        }

        // Last, so a listener reacting to the transition reads a quest that has already been paid
        // and filed rather than one still being settled.
        if (getState() != previousState) {
            HytaleServer.get()
                        .getEventBus()
                        .dispatchFor(QuestStateChangedEvent.class, getId())
                        .dispatch(new QuestStateChangedEvent(this, previousState));
        }
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
        // Handed the quest again after walking away from it: they are running it, not done with it
        if (!membership.move(playerId, Membership.Status.JOINED)) return false;
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
        if (!membership.is(playerId, Membership.Status.JOINED)) return false;

        membership.move(playerId, Membership.Status.LEFT);
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
        if (!membership.is(playerId, Membership.Status.JOINED)) return false;

        membership.move(playerId, Membership.Status.ABANDONED);
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
        return membership.is(playerId, Membership.Status.ABANDONED);
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
        return membership.getAbandoned();
    }

    /**
     * @return who shares this quest beyond its players, {@code null} for one they hold on their own.
     */
    @Nullable
    public QuestScope getScope() {
        return scope;
    }

    /**
     * Written by the scope holding the quest, never by the quest itself.
     */
    public Q setScope(@Nullable QuestScope scope) {
        this.scope = scope;
        markDirty();
        return self();
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
     * Written before the quest is registered, by the assignment handing it out or by a chain
     * passing its own on.
     */
    public Q setOrigin(@Nullable QuestOrigin origin) {
        this.origin = origin;
        markDirty();
        return self();
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
     * Written before the quest is registered, by whatever hands it over, so everything hearing of
     * it already knows which completion opened it.
     */
    public Q setGrantedBy(@Nullable UUID grantedBy) {
        this.grantedBy = grantedBy;
        markDirty();
        return self();
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

    /**
     * Records the asset class a quest type casts its asset to, which {@link #findAssetMismatch()}
     * checks a quest read back against.
     */
    public static void registerAssetClass(@Nonnull Class<?> questClass, @Nonnull Class<? extends OpenQuestAsset> assetClass) {
        ASSET_CLASSES.put(questClass, assetClass);
    }

    /**
     * @return why this quest cannot run on the asset now under its id, gone or of another type than
     * the one it casts to, or {@code null} if it can. A quest built without an asset id never had
     * one to lose.
     */
    @Nullable
    public String findAssetMismatch() {
        // Straight from the asset store: the overrides of getAsset() cast, which is the very failure
        return assetId == null ? null : findAssetMismatch(getClass(), assetId, OpenQuestAsset.getAsset(assetId));
    }

    /**
     * The check itself, apart from the asset store the instance method reads.
     */
    @Nullable
    static String findAssetMismatch(@Nonnull Class<?> questClass, @Nonnull String assetId, @Nullable OpenQuestAsset asset) {
        if (asset == null) return "no quest asset is named " + assetId;

        Class<? extends OpenQuestAsset> expected = ASSET_CLASSES.get(questClass);
        if (expected == null || expected.isInstance(asset)) return null;

        return assetId + " is a " + asset.getClass().getSimpleName() + " now, where " + questClass.getSimpleName() + " reads a " + expected.getSimpleName();
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

        return QuestProgressionService.get().getQuest(parentId) instanceof AbstractCompositeQuestProgression<?> parent ? parent : null;
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
     * Written by {@link AbstractCompositeQuestProgression} alone, as it adopts or claims a step.
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
     * Declares a tag carrying nothing.
     *
     * @return {@code false} if the quest already carried it.
     */
    public boolean addTag(@Nonnull String tag) {
        return addTag(tag, NO_TAG_VALUES);
    }

    /**
     * Writes a tag and what it carries, replacing whatever the quest held under it.
     *
     * @return {@code false} if the quest already carried exactly that.
     */
    public boolean addTag(@Nonnull String tag, @Nonnull String... values) {
        String[] previous = tags.put(tag, values);
        if (previous != null && Arrays.equals(previous, values)) return false;

        markDirty();
        return true;
    }

    /**
     * @return {@code false} if the quest did not carry the tag.
     */
    public boolean removeTag(@Nonnull String tag) {
        if (tags.remove(tag) == null) return false;
        markDirty();

        return true;
    }

    /**
     * @return the live, mutable tags written on this quest alone, without those of its asset.
     */
    public Map<String, String[]> getTags() {
        return tags;
    }

    /**
     * @return the ids of the players this quest is assigned to, read only: moves go through
     * {@link #addPlayer} and {@link #removePlayer}.
     */
    @Nonnull
    public Set<UUID> getPlayers() {
        return membership.getPlayers();
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
}
