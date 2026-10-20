package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What one copy of a quest knows of the stored copy it stands on, and the operations made here
 * since: how a quest other servers write too is written without undoing what they wrote.
 * Guarded by the quest itself.
 */
public final class QuestReplica<Q extends AbstractQuestProgression<Q>> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
     * Quest types already reported for changing a shared quest past {@link AbstractQuestProgression#apply}, reported once.
     */
    private static final Set<Class<?>> REPORTED_UNKEPT = ConcurrentHashMap.newKeySet();

    @Nonnull
    private final Q quest;

    /**
     * The stored copy the quest stands on, which it may have moved past with changes not written yet.
     */
    @Nonnull
    private volatile Stored stored = Stored.NONE;

    /**
     * The operations made here since the stored copy, in order, for a quest other servers write
     * too: each is made again over whatever they stored meanwhile, which a copy written over it
     * would undo.
     */
    private final List<QuestOperation<? super Q>> pending = new ArrayList<>();

    QuestReplica(@Nonnull Q quest) {
        this.quest = quest;
    }

    /**
     * Refuses a quest type with a final field it is written as, which no stored copy could be
     * taken on through.
     */
    public static void checkStoredFields(@Nonnull Class<?> questClass) {
        STORED_FIELDS.get(questClass);
    }

    /**
     * Keeps an operation just made, for a quest other servers write too. What this copy changed
     * before and nothing could make again, before the quest was shared or past
     * {@link AbstractQuestProgression#apply}, rides along instead as a copy of the whole quest.
     *
     * @param unsaved whether the quest was waiting to be written before the operation
     * @return whether the operation was kept, the quest being shared
     */
    boolean keep(@Nonnull QuestOperation<? super Q> operation, boolean unsaved) {
        if (!quest.isShared()) {
            // Written whole from now on: what was kept would be made twice were it shared again
            pending.clear();
            return false;
        }

        pending.add(pending.isEmpty() && unsaved ? new Overwrite(copy()) : operation);
        return true;
    }

    /**
     * Warns, once per quest type, that a shared quest changed past {@link AbstractQuestProgression#apply}.
     */
    void reportUnkept() {
        if (stored.version() > 0 && quest.isShared() && REPORTED_UNKEPT.add(quest.getClass())) {
            LOGGER.atWarning().log("A %s other servers write too was changed past apply(): written as a copy of the whole quest, it may undo what another server wrote", quest.getClass().getSimpleName());
        }
    }

    /**
     * @return an operation writing the quest, as it stands now, over whichever copy it is made
     * on: how a quest whose copy here has the last word is written over one that moved under it.
     */
    @Nonnull
    public QuestOperation<AbstractQuestProgression<?>> overwrite() {
        return new Overwrite(copy());
    }

    /**
     * Writes that copy over whichever it is made on, every field of it.
     */
    private record Overwrite(@Nonnull AbstractQuestProgression<?> snapshot) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            // A copy of the copy each time: one write may be tried again on another stored copy
            quest.getReplica().copyStoredFields(snapshot.getReplica().copy());
            return true;
        }
    }

    /**
     * @return the stored copy the quest stands on, {@link Stored#NONE} for a quest never written.
     */
    @Nonnull
    public Stored getStored() {
        return stored;
    }

    /**
     * @return the version of the stored copy the quest stands on, {@code 0} for a quest never written.
     */
    public long getVersion() {
        return stored.version();
    }

    /**
     * @return where the stored copy stands, which the quest may have moved past.
     */
    @Nonnull
    public QuestState getState() {
        return stored.state();
    }

    /**
     * Notes what the quest was written or read back as. For the storage alone.
     */
    public void markStored(@Nonnull Stored stored) {
        this.stored = stored;
    }

    /**
     * @return the operations waiting to be written, in order, those that add up made one: what a
     * quest keeps between writes stays as small as what it would write. A quest waiting to be
     * written with none kept, changed past {@link AbstractQuestProgression#apply}, hands a copy of itself instead.
     */
    @Nonnull
    public List<QuestOperation<? super Q>> getPendingOperations() {
        synchronized (quest) {
            if (pending.isEmpty() && quest.hasChanges() && quest.isShared()) pending.add(new Overwrite(copy()));

            // Merged here rather than as they are kept: a write already under way holds the ones before
            List<QuestOperation<? super Q>> merged = new ArrayList<>(pending.size());
            for (QuestOperation<? super Q> operation : pending) {
                QuestOperation<? super Q> both = merged.isEmpty() ? null : merged.getLast().followedBy(operation);
                if (both != null) {
                    merged.set(merged.size() - 1, both);
                } else {
                    merged.add(operation);
                }
            }
            pending.clear();
            pending.addAll(merged);

            return List.copyOf(pending);
        }
    }

    /**
     * Makes on the quest an operation another copy of it kept, telling nobody: how a stored copy
     * takes on what a server is waiting to write.
     *
     * @return whether it changed anything.
     */
    @SuppressWarnings("unchecked")
    public boolean replay(@Nonnull QuestOperation<?> operation) {
        return quest.make((QuestOperation<? super Q>) operation);
    }

    /**
     * Takes on a stored copy, written or read back, then makes again on it the operations made
     * here that it does not carry: the quest stands on what is stored, plus what is still to write.
     *
     * @param written how many of the operations waiting here the stored copy carries, the first ones
     */
    public void restack(@Nonnull AbstractQuestProgression<?> from, int written) {
        synchronized (quest) {
            pending.subList(0, written).clear();

            copyStoredFields(from);
            markStored(from.getReplica().stored);

            for (QuestOperation<? super Q> operation : pending) quest.make(operation);
            quest.dirty = !pending.isEmpty();
        }
    }

    /**
     * Takes every field the other copy is written as.
     */
    private void copyStoredFields(@Nonnull AbstractQuestProgression<?> from) {
        if (from.getClass() != quest.getClass()) throw new IllegalArgumentException("Quest " + quest.getId() + " is a " + quest.getClass().getSimpleName() + ", not a " + from.getClass().getSimpleName());

        try {
            for (Field field : STORED_FIELDS.get(quest.getClass())) {
                field.set(quest, field.get(from));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to take on a stored copy of quest " + quest.getId(), e);
        }
    }

    /**
     * @return a copy of what the quest is written as, by way of its codec, standing on the same
     * stored version.
     */
    @Nonnull
    public AbstractQuestProgression<?> copy() {
        ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
        AbstractQuestProgression<?> copy = AbstractQuestProgression.CODEC.decode(AbstractQuestProgression.CODEC.encode(quest, extraInfo), extraInfo);
        copy.getReplica().markStored(stored);
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
}
