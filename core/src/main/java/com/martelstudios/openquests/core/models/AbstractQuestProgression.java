package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.hypixel.hytale.codec.lookup.CodecMapCodec;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAbandonedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAddedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.sync.QuestSync;
import com.martelstudios.openquests.core.visitors.QuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.ArrayList;
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
import java.util.function.Predicate;

/**
 * Defines the quest progression. Extend it to create new quest types or to add runtime progression data.
 * Avoid using it to declare static serialized data. Look at {@link OpenQuestAsset} for static serialized data declaration.
 */
public abstract class AbstractQuestProgression<Q extends AbstractQuestProgression<Q>> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final CodecMapCodec<AbstractQuestProgression<?>> CODEC = new CodecMapCodec<>("Type");

    private static final KeyedCodec<UUID[]> PLAYERS_CODEC = new KeyedCodec<>("Players", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));
    private static final KeyedCodec<UUID[]> ABANDONED_PLAYERS_CODEC = new KeyedCodec<>("AbandonedPlayers", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new));

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
     * The fields each quest type is written as, its own and those it inherits: whatever is neither
     * static nor transient.
     */
    private static final ClassValue<List<Field>> STORED_FIELDS = new ClassValue<>() {
        @Override
        protected List<Field> computeValue(@Nonnull Class<?> type) {
            List<Field> fields = new ArrayList<>();
            for (Class<?> c = type; c != null && AbstractQuestProgression.class.isAssignableFrom(c); c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) continue;
                    if (Modifier.isFinal(modifiers)) {
                        throw new IllegalStateException(c.getName() + "." + field.getName() + " is final: a quest takes on a stored copy field by field, so a field it is written as cannot be. Make it transient if it is not written.");
                    }
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            return List.copyOf(fields);
        }
    };

    /**
     * Quest types already reported for changing a shared quest past {@link #change}, reported once.
     */
    private static final Set<Class<?>> REPORTED_UNKEPT = ConcurrentHashMap.newKeySet();

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
                                                                                        .append(new KeyedCodec<>("State", new EnumCodec<>(QuestState.class)), (quest, state) -> quest.state = state, quest -> quest.state)
                                                                                        .add()
                                                                                        .append(new KeyedCodec<>("Transitions", Codec.INTEGER), (quest, count) -> quest.transitions = count, quest -> Integer.valueOf(quest.transitions))
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
    protected UUID id = UUID.randomUUID();

    /**
     * Ids of the players running the quest.
     */
    protected Set<UUID> players = ConcurrentHashMap.newKeySet();

    /**
     * Ids of the players who gave it up, never also running it.
     */
    protected Set<UUID> abandonedPlayers = ConcurrentHashMap.newKeySet();

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
     * How many times the quest changed state, written with it: what tells an older outcome from a
     * newer one, for a group keeping how its steps went.
     */
    private int transitions;

    /**
     * The stored copy this one stands on, which this one may have moved past with changes not
     * written yet.
     */
    @Nonnull
    private transient volatile Stored storedCopy = Stored.NONE;

    /**
     * The changes made here since the stored copy, in order, for a quest other servers write too:
     * each is made again over whatever they stored meanwhile, which a copy written over it would
     * undo. Guarded by the quest itself.
     */
    private final transient List<Change<Q>> pending = new ArrayList<>();

    /**
     * Grows with every change, so that one update can tell whether it changed anything.
     */
    private transient int changeCount;

    /**
     * Above zero while a change is being made, where marking the quest dirty is expected.
     */
    private transient int applying;

    /**
     * Above zero while a change kept on another copy is made again on this one, which then tells
     * nobody and reaches no other quest: that was done where the change was first made.
     */
    private transient int replaying;

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
        return change(quest -> {
            if (Objects.equals(quest.track, track)) return false;

            quest.track = track;
            return true;
        });
    }

    /**
     * Applies the visitor, then settles the quest before anyone hears of it. A player's action is
     * first put to the constraints of the asset, and leaves no trace if one objects. A quest other
     * servers write too hears of a change of state once the write making it for all of them is done.
     *
     * @param visitor the visitor to apply
     */
    public void update(QuestVisitor<Q> visitor) {
        UUID actorId = visitor.getActorId();
        if (actorId != null && !allowsProgress(actorId)) return;

        QuestState previousState;
        QuestState newState;
        int newTransitions;
        boolean kept;
        synchronized (this) {
            previousState = state;
            int before = changeCount;
            boolean unsaved = dirty;

            applying++;
            try {
                progress(visitor);
            } finally {
                applying--;
            }
            if (changeCount == before) return;

            newState = state;
            newTransitions = transitions;
            kept = keep(quest -> {
                AbstractQuestProgression<Q> same = quest;
                int unchanged = same.changeCount;
                same.progress(visitor);
                return same.changeCount != unchanged;
            }, unsaved);
        }

        announceUpdate();

        if (newState == previousState) return;

        if (kept) {
            QuestSync.writeSoon(this);
        } else {
            announce(previousState, newState, newTransitions, true);
        }
    }

    /**
     * Applies the visitor and settles the quest, telling nobody: what an update does to this copy,
     * and makes again over the stored one. A quest that is over takes no more.
     */
    private void progress(@Nonnull QuestVisitor<Q> visitor) {
        if (isOver()) return;

        QuestState previousState = state;
        visitor.progress(self());

        // If no player remains and some has abandoned set the quest as abandoned
        if (!isCompleted() && getPlayers().isEmpty() && !getAbandonedPlayers().isEmpty()) {
            setState(QuestState.ABANDONED).markDirty();
        }

        if (state != previousState) {
            completedAt = isCompleted() ? Instant.now() : null;
            transitions++;
            markDirty();
        }
    }

    /**
     * Makes a change to the quest. Every write to a quest goes through here or {@link #update}: a
     * quest other servers write too keeps the change, to make it again over whatever they stored
     * meanwhile, where writing this copy over theirs would undo it.
     *
     * @param change makes the change on the quest it is handed, and says whether it changed anything
     * @return whether it changed anything, the quest then waiting to be written
     */
    protected final boolean change(@Nonnull Change<Q> change) {
        synchronized (this) {
            boolean unsaved = dirty;

            applying++;
            try {
                if (!change.apply(self())) return false;
                markDirty();
            } finally {
                applying--;
            }

            keep(change, unsaved);
        }
        return true;
    }

    /**
     * Keeps a change just made, for a quest other servers write too. What this copy changed before
     * and nothing could make again, before the quest was shared or past {@link #change}, rides
     * along instead as a copy of the whole quest.
     *
     * @param unsaved whether the quest was waiting to be written before the change
     * @return whether the change was kept, the quest being shared
     */
    private boolean keep(@Nonnull Change<Q> change, boolean unsaved) {
        if (!QuestSync.isShared(this)) {
            // Written whole from now on: what was kept would be made twice were it shared again
            pending.clear();
            return false;
        }

        pending.add(pending.isEmpty() && unsaved ? overwriteWith(copy()) : change);
        return true;
    }

    /**
     * @return a change writing this quest, as it stands now, over whichever copy it is made on:
     * how a quest whose copy here has the last word is written over one that moved under it.
     */
    @Nonnull
    public Change<Q> overwrite() {
        return overwriteWith(copy());
    }

    /**
     * @return a change writing that copy over whichever it is made on, every field of it.
     */
    @Nonnull
    private Change<Q> overwriteWith(@Nonnull AbstractQuestProgression<?> snapshot) {
        // A copy of the copy each time: one write may be tried again on another stored copy
        return quest -> {
            ((AbstractQuestProgression<Q>) quest).copyStoredFields(snapshot.copy());
            return true;
        };
    }

    /**
     * Tells everyone here the quest changed state: as it changes, for a quest only this server
     * writes; once written, for one other servers write too, by the write that made the change or
     * brought news of it.
     *
     * @param transitions how many changes of state the quest went through, this one included
     * @param madeHere whether this server made the change, and pays what an end pays
     */
    public void announce(@Nonnull QuestState previousState, @Nonnull QuestState newState, int transitions, boolean madeHere) {
        if (newState.isCompleted()) {
            HytaleServer.get()
                        .getEventBus()
                        .dispatchFor(QuestCompletedEvent.class, getId())
                        .dispatch(new QuestCompletedEvent(this, newState, madeHere));
        }

        // Last, so a listener reacting to the transition reads a quest that has already been paid
        // and filed rather than one still being settled.
        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestStateChangedEvent.class, getId())
                    .dispatch(new QuestStateChangedEvent(this, previousState, newState, transitions));
    }

    /**
     * Tells whatever draws the quest that it moved.
     */
    public void announceUpdate() {
        HytaleServer.get()
                    .getEventBus()
                    .dispatchFor(QuestUpdatedEvent.class, getId())
                    .dispatch(new QuestUpdatedEvent(this));
    }

    /**
     * @return the stored copy this one stands on, {@link Stored#NONE} for a quest never written.
     */
    @Nonnull
    public Stored getStored() {
        return storedCopy;
    }

    /**
     * @return the version of the stored copy this one stands on, {@code 0} for a quest never written.
     */
    public long getStoredVersion() {
        return storedCopy.version();
    }

    /**
     * @return where the stored copy stands, which this one may have moved past.
     */
    @Nonnull
    public QuestState getStoredState() {
        return storedCopy.state();
    }

    /**
     * Notes what this copy was written or read back as. For the storage alone.
     */
    public void markStored(@Nonnull Stored stored) {
        storedCopy = stored;
    }

    /**
     * @return the changes waiting to be written, in order. A quest waiting to be written with none
     * kept, changed past {@link #change}, hands a copy of itself instead.
     */
    @Nonnull
    public synchronized List<Change<Q>> getPendingChanges() {
        if (pending.isEmpty() && dirty && QuestSync.isShared(this)) pending.add(overwriteWith(copy()));

        return List.copyOf(pending);
    }

    /**
     * Makes on this copy a change another copy of the quest kept, telling nobody: how a stored
     * copy takes on what a server is waiting to write.
     *
     * @return whether it changed anything.
     */
    @SuppressWarnings("unchecked")
    public boolean replay(@Nonnull Change<?> change) {
        applying++;
        replaying++;
        try {
            if (!((Change<Q>) change).apply(self())) return false;
            markDirty();
            return true;
        } finally {
            replaying--;
            applying--;
        }
    }

    /**
     * @return whether a change kept on another copy is being made again on this one.
     */
    boolean isReplaying() {
        return replaying > 0;
    }

    /**
     * Takes on a stored copy, written or read back, then makes again on it the changes made here
     * that it does not carry: this copy stands on what is stored, plus what is still to write.
     *
     * @param written how many of the changes waiting here the stored copy carries, the first ones
     */
    public synchronized void restack(@Nonnull AbstractQuestProgression<?> stored, int written) {
        pending.subList(0, written).clear();

        copyStoredFields(stored);
        markStored(stored.storedCopy);

        for (Change<Q> change : pending) replay(change);
        dirty = !pending.isEmpty();
    }

    /**
     * Takes every field the other copy is written as.
     */
    private void copyStoredFields(@Nonnull AbstractQuestProgression<?> from) {
        if (from.getClass() != getClass()) throw new IllegalArgumentException("Quest " + id + " is a " + getClass().getSimpleName() + ", not a " + from.getClass().getSimpleName());

        try {
            for (Field field : STORED_FIELDS.get(getClass())) {
                field.set(this, field.get(from));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to take on a stored copy of quest " + id, e);
        }
    }

    /**
     * @return a copy of what this quest is written as, by way of its codec, standing on the same
     * stored version.
     */
    @Nonnull
    public AbstractQuestProgression<?> copy() {
        ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
        AbstractQuestProgression<?> copy = CODEC.decode(CODEC.encode(this, extraInfo), extraInfo);
        copy.markStored(storedCopy);
        return copy;
    }

    /**
     * What the stored copy a quest stands on holds: its version, its state, and who held it, which
     * the storage moves the quest's links from as it writes the next version.
     *
     * @param version {@code 0} for a quest never written
     */
    public record Stored(long version, @Nonnull QuestState state, @Nonnull Set<UUID> players, @Nonnull Set<UUID> abandoned) {

        public static final Stored NONE = new Stored(0, QuestState.IN_PROGRESS, Set.of(), Set.of());

        /**
         * @return what that quest holds now, as stored at that version.
         */
        @Nonnull
        public static Stored of(@Nonnull AbstractQuestProgression<?> quest, long version) {
            return new Stored(version, quest.getState(), Set.copyOf(quest.getPlayers()), Set.copyOf(quest.getAbandonedPlayers()));
        }
    }

    /**
     * @return how many times the quest changed state, which orders what became of it.
     */
    public int getTransitions() {
        return transitions;
    }

    /**
     * One change to a quest, which a quest other servers write too keeps, to make it again on
     * whichever stored copy it is written over.
     */
    @FunctionalInterface
    public interface Change<Q extends AbstractQuestProgression<Q>> {

        /**
         * Reads what it changes off the quest it is handed, never off the copy it was first made
         * on: the two may differ.
         *
         * @return whether it changed anything.
         */
        boolean apply(@Nonnull Q quest);
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
        boolean added = change(quest -> {
            if (!quest.players.add(playerId)) return false;

            // Handed the quest again after walking away from it: they are running it, not done with it
            quest.abandonedPlayers.remove(playerId);
            return true;
        });
        if (!added || isReplaying()) return added;

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
        boolean removed = change(quest -> quest.players.remove(playerId));
        if (!removed || isReplaying()) return removed;

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
        boolean abandoned = change(quest -> {
            if (!quest.players.remove(playerId)) return false;

            quest.abandonedPlayers.add(playerId);
            return true;
        });
        if (!abandoned || isReplaying()) return abandoned;

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
        return state.isCompleted();
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
     * Written by the scope holding the quest, never by the quest itself.
     */
    public Q setScope(@Nullable QuestScope scope) {
        change(quest -> {
            quest.scope = scope;
            return true;
        });
        return self();
    }

    /**
     * Changes the scope the quest carries in place, such as the worlds it reached.
     *
     * @param change says whether it changed the scope
     * @return whether it changed the scope, the quest then waiting to be written
     */
    public boolean changeScope(@Nonnull Predicate<QuestScope> change) {
        return change(quest -> quest.scope != null && change.test(quest.scope));
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
        change(quest -> {
            quest.origin = origin;
            return true;
        });
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
        change(quest -> {
            quest.grantedBy = grantedBy;
            return true;
        });
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
     * checks a quest read back against. Refuses a type with a final field it is written as, which
     * no stored copy could be taken on through.
     */
    public static void registerAssetClass(@Nonnull Class<?> questClass, @Nonnull Class<? extends OpenQuestAsset> assetClass) {
        STORED_FIELDS.get(questClass);
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
        return change(quest -> !Arrays.equals(quest.tags.put(tag, values), values));
    }

    /**
     * @return {@code false} if the quest did not carry the tag.
     */
    public boolean removeTag(@Nonnull String tag) {
        return change(quest -> quest.tags.remove(tag) != null);
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
        return Collections.unmodifiableSet(players);
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
     * Marks this quest as needing to be persisted on the next save pass. A quest other servers
     * write too changes through {@link #change} or {@link #update}: a change made past them is
     * reported, and only ever written as a copy of the whole quest.
     */
    public void markDirty() {
        this.dirty = true;
        changeCount++;

        if (applying == 0 && getStoredVersion() > 0 && QuestSync.isShared(this) && REPORTED_UNKEPT.add(getClass())) {
            LOGGER.atWarning().log("A %s other servers write too was changed past change(): written as a copy of the whole quest, it may undo what another server wrote", getClass().getSimpleName());
        }
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
