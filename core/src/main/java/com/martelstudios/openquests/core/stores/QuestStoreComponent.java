package com.martelstudios.openquests.core.stores;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The quests of one player while they are online: which ones they take part in, which of the
 * catalogue they have already been offered, and how each asset ended for them so far.
 *
 * <p>Never written to the player's entity file: where a quest is kept is the
 * {@link com.martelstudios.openquests.core.persistence.QuestStorage}'s business, and this is the
 * reverse index it fills on connection.
 *
 * <p>It stays a component because it is the marker every quest system queries on.
 */
public class QuestStoreComponent implements Component<EntityStore> {

    private QuestsRecord quests = new QuestsRecord();

    /**
     * What the assignments handed this player, by assignment and then by quest. Only the hand-outs
     * are kept between sessions, not the quests made from them.
     */
    private final AssignmentRecords assignments = new AssignmentRecords();

    /**
     * How each asset ended for this player so far, by asset id. Counted as quests end, whether
     * or not the quests themselves are kept.
     */
    private final Map<String, QuestCompletions> completions = new ConcurrentHashMap<>();

    /**
     * Whether this player tracks a quest, by quest id, for the quests they said so of: a quest
     * many players hold is tracked by each of them their own way. One left out is the asset's call.
     */
    private final Map<UUID, Boolean> tracking = new ConcurrentHashMap<>();

    /**
     * Set when something here changed and the player's record is owed a write.
     */
    private transient boolean dirty;

    public QuestStoreComponent() {

    }

    public QuestStoreComponent(@Nonnull QuestStoreComponent other) {
        this.quests = other.quests.clone();
        this.assignments.replaceAll(other.assignments.snapshot());
        this.completions.putAll(other.completions);
        this.tracking.putAll(other.tracking);
        this.dirty = other.dirty;
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
     * @return the index of every quest of this player.
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

    /**
     * @return the live records of what the assignments handed this player.
     */
    @Nonnull
    public AssignmentRecords getAssignments() {
        return assignments;
    }

    /**
     * @return the live map of how each asset ended for this player, by asset id.
     */
    @Nonnull
    public Map<String, QuestCompletions> getCompletions() {
        return completions;
    }

    /**
     * @return how quests from that asset ended for this player, {@link QuestCompletions#NONE} if
     * none has yet.
     */
    @Nonnull
    public QuestCompletions getCompletions(@Nonnull String assetId) {
        return completions.getOrDefault(assetId, QuestCompletions.NONE);
    }

    /**
     * Counts one more quest from that asset ended that way.
     */
    public void recordCompletion(@Nonnull String assetId, @Nonnull QuestState outcome, @Nullable Instant startedAt, @Nullable Instant completedAt) {
        completions.compute(assetId, (id, current) -> (current == null ? QuestCompletions.NONE : current).record(outcome, startedAt, completedAt));
        markDirty();
    }

    /**
     * @return whether this player said they track that quest, {@code null} while they said nothing
     * and its asset answers.
     */
    @Nullable
    public Boolean getTracking(@Nonnull UUID questId) {
        return tracking.get(questId);
    }

    /**
     * @param track {@code null} hands the answer back to the asset, which is not the same as saying
     * no: the player stops having an opinion of their own.
     */
    public void setTracking(@Nonnull UUID questId, @Nullable Boolean track) {
        Boolean before = track == null ? tracking.remove(questId) : tracking.put(questId, track);
        if (!Objects.equals(before, track)) markDirty();
    }

    /**
     * @return what this player said of tracking each quest they still hold, by quest id, for their
     * record: what they said of a quest gone goes with it.
     */
    @Nonnull
    public Map<UUID, Boolean> getTracking() {
        Map<UUID, Boolean> held = new HashMap<>(tracking);
        held.keySet().retainAll(getQuestIds());
        return held;
    }

    /**
     * Takes over what was read back for this player, which is how a session starts.
     *
     * @param questIds the quests that answered, not the ids their record listed, so a dead id is
     * dropped here rather than carried another session.
     */
    public void restore(@Nonnull Set<UUID> questIds, @Nonnull Map<String, Map<String, AssignmentRecord>> assignments, @Nonnull Map<String, QuestCompletions> completions, @Nonnull Map<UUID, Boolean> tracking) {
        quests.replaceAll(questIds);
        this.assignments.replaceAll(assignments);
        this.completions.putAll(completions);
        this.tracking.putAll(tracking);
        dirty = false;
    }

    public void markDirty() {
        this.dirty = true;
    }

    /**
     * @return {@code true} if this player's record changed since the last write, without clearing
     * the flag.
     */
    public boolean hasChanges() {
        return dirty;
    }

    /**
     * @return {@code true} if this player's record changed since the last call, clearing the flag.
     */
    public boolean consumeChanges() {
        if (!dirty) return false;
        dirty = false;
        return true;
    }
}
