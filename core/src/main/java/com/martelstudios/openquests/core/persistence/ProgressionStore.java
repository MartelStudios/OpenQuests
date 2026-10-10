package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The progressions, one stored copy per quest with a version that grows with every write, each
 * only ever written over the version it was read at. A quest only one server writes is written
 * whole; one several servers write has each server make its own changes on the latest version, so
 * nobody's change is ever undone.
 */
public interface ProgressionStore {

    /**
     * How many times a change is made on a newer version before it is left for the next pass.
     */
    int COMMIT_ATTEMPTS = 10;

    /**
     * @return the quest under that id, standing on the version it was read at, or {@code null} for
     * one nothing answers to.
     */
    @Nullable
    AbstractQuestProgression<?> loadProgression(@Nonnull UUID questId);

    /**
     * Ids nothing answers to are left out, so the result may be shorter than what was asked for.
     */
    @Nonnull
    List<AbstractQuestProgression<?>> loadProgressions(@Nonnull Collection<UUID> questIds);

    /**
     * Every quest this player takes part in, the ones they gave up included. The quest has the
     * last word on who holds it: one whose players no longer list this one is left out.
     */
    @Nonnull
    List<AbstractQuestProgression<?>> loadPlayerProgressions(@Nonnull UUID playerId);

    /**
     * Every quest the backend holds, for a migration or an audit.
     */
    @Nonnull
    List<AbstractQuestProgression<?>> loadAllProgressions();

    /**
     * Writes each quest over the version it stands on, a new one created, as one batch with who
     * holds them. Each quest written then stands on the version it is stored at.
     *
     * @return the ids of those that another write moved first, left as stored: never written over.
     */
    @Nonnull
    Set<UUID> writeProgressions(@Nonnull Collection<? extends AbstractQuestProgression<?>> quests);

    /**
     * Makes changes on the latest version and writes it over that version alone, reading again and
     * making them again whenever another write came first. Changes that make no difference write
     * nothing.
     *
     * @param changes makes the changes on the stored copy it is handed and says whether they
     * changed it, which may happen more than once: the last copy handed over is the one kept
     * @return that copy, standing on the version it is stored at, or {@code null} if nothing is
     * stored under that id any more
     */
    @Nullable
    default AbstractQuestProgression<?> commitProgression(@Nonnull UUID questId, @Nonnull Predicate<AbstractQuestProgression<?>> changes) {
        for (int attempt = 0; attempt < COMMIT_ATTEMPTS; attempt++) {
            AbstractQuestProgression<?> stored = loadProgression(questId);
            if (stored == null) return null;

            // Made already by whoever wrote this version: nothing for the others to read again
            if (!changes.test(stored)) return stored;

            if (writeProgressions(List.of(stored)).isEmpty()) return stored;
        }
        throw new QuestStorageException("Quest " + questId + " kept being written by other servers; its changes wait for the next pass");
    }

    /**
     * @return the version each of those quests is stored at, those with nothing stored left out.
     */
    @Nonnull
    Map<UUID, Long> loadVersions(@Nonnull Collection<UUID> questIds);

    /**
     * Does away with a quest and every link to it. Takes the quest rather than its id: a backend
     * may need the players it named, and nothing else still knows who they were.
     *
     * <p>A backend several servers share keeps an ended quest a while longer: a server still
     * running the quest learns it ended, rather than ending it a second time.
     */
    void deleteProgression(@Nonnull AbstractQuestProgression<?> quest);
}
