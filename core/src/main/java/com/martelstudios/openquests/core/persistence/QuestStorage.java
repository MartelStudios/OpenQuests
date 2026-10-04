package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.AssignmentRecords;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Everything the quest system keeps between restarts, and the only thing the rest of the core
 * knows about where it is kept. A backend answers for three kinds of record: the progressions
 * themselves, the named sets of ids a scope hands out, and what one player carries besides their
 * quests.
 *
 * <p>Every progression is one record, whoever holds it: a backend lays them out as it likes
 * underneath, but nothing above may be told a quest held by one player is written differently
 * from one shared by ten.
 *
 * <p>Reads block, and writes are durable once they return. Both run off the world threads except
 * on connection and on world entry.
 */
public interface QuestStorage extends AutoCloseable {

    /**
     * Opens the backend and brings it up to the shape this version expects.
     *
     * @throws QuestStorageException if it cannot be reached or prepared, which stops the server
     * rather than run it against storage nobody can write to.
     */
    void start();

    /**
     * Closes the backend. Whatever is still buffered is written first.
     */
    @Override
    void close();

    /**
     * @return a short name for logs, such as {@code Disk} or {@code Jdbc}.
     */
    @Nonnull
    String getId();

    /**
     * @return the quest under that id, or {@code null} for one nothing answers to.
     */
    @Nullable
    AbstractQuestProgression<?> loadProgression(@Nonnull UUID questId);

    /**
     * Ids nothing answers to are left out, so the result may be shorter than what was asked for.
     */
    @Nonnull
    List<AbstractQuestProgression<?>> loadProgressions(@Nonnull Collection<UUID> questIds);

    /**
     * Every quest this player takes part in, the ones they gave up included.
     *
     * <p>The quest has the last word on who holds it, so one an index names but whose players no
     * longer list this one is left out: a stale index corrects itself rather than being reconciled.
     */
    @Nonnull
    default List<AbstractQuestProgression<?>> loadPlayerProgressions(@Nonnull UUID playerId) {
        List<AbstractQuestProgression<?>> held = new ArrayList<>();

        for (AbstractQuestProgression<?> quest : loadProgressions(loadPlayer(playerId).getQuestIds())) {
            if (quest.getPlayers().contains(playerId) || quest.getAbandonedPlayers().contains(playerId)) held.add(quest);
        }
        return held;
    }

    /**
     * Every quest the backend holds, for a migration or an audit. A shared database holds what every
     * server on it ever wrote.
     */
    @Nonnull
    List<AbstractQuestProgression<?>> loadAllProgressions();

    /**
     * Writes quests down as one batch. Who holds a quest is read off the quest itself, so dropping
     * a player from one unlinks them by this call alone.
     */
    void saveProgressions(@Nonnull Collection<AbstractQuestProgression<?>> quests);

    /**
     * Does away with a quest and every link to it. Takes the quest rather than its id: a backend
     * may need the players it named, and nothing else still knows who they were.
     */
    void deleteProgression(@Nonnull AbstractQuestProgression<?> quest);

    /**
     * A named set of quest ids, which is how a scope remembers what it handed out. The universe
     * scope uses one key, a world scope one key per world.
     *
     * @return the ids under that key, empty for a key nothing answers to. Never {@code null}.
     */
    @Nonnull
    Set<UUID> loadIndex(@Nonnull String indexKey);

    /**
     * Replaces what is under that key. An empty set leaves the key holding nothing rather than
     * leaving it as it was.
     */
    void saveIndex(@Nonnull String indexKey, @Nonnull Set<UUID> questIds);

    /**
     * Lets go of a key nothing will read again, such as a closed instance's. A backend keeping
     * nothing per key can leave it at an empty index.
     */
    default void deleteIndex(@Nonnull String indexKey) {
        saveIndex(indexKey, Set.of());
    }

    /**
     * Reads back what the assignments handed a holder many players share: a world, a group of
     * worlds, the server. A player's own are part of their record.
     *
     * @return empty for a holder handed nothing yet.
     */
    @Nonnull
    AssignmentRecords loadAssignments(@Nonnull String holderKey);

    /**
     * Writes one record only if what is stored still matches what the caller read, so servers
     * sharing a holder never both hand the same occasion out.
     *
     * @param expected what the caller read, {@code null} when there was nothing
     * @return {@code false} if something else wrote in between, nothing then written.
     */
    boolean claimAssignment(@Nonnull String holderKey, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nullable AssignmentRecord expected, @Nonnull AssignmentRecord next);

    /**
     * Lets go of a holder nothing will hand anything to again, such as a closed world.
     */
    void deleteAssignments(@Nonnull String holderKey);

    /**
     * What a player carries besides the quests themselves: which of their quests to look for, the
     * catalogue they have already been offered, what they are still owed, and how each asset
     * ended for them so far.
     *
     * @return their record, empty for a player who has never held a quest. Never {@code null}.
     */
    @Nonnull
    PlayerQuestRecord loadPlayer(@Nonnull UUID playerId);

    /**
     * Replaces a player's record whole.
     */
    void savePlayer(@Nonnull UUID playerId, @Nonnull PlayerQuestRecord record);

    /**
     * Writes one debt into a player's record atomically, for the player nobody is holding. Reading
     * and writing the whole record around it would race the save pass.
     *
     * <p>Replaces the entry under the same quest id: a debt partly paid is still one debt.
     */
    void addPendingRewards(@Nonnull UUID playerId, @Nonnull PendingRewards owed);

    /**
     * Counts one outcome into a player's record atomically, for the player nobody is holding, the
     * same way as a debt.
     */
    void recordCompletion(@Nonnull UUID playerId, @Nonnull String assetId, @Nonnull QuestState outcome, @Nullable Instant startedAt, @Nullable Instant completedAt);
}
