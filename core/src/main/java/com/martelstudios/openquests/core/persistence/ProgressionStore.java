package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * The progressions, one stored copy per quest with a version that grows with every write. A quest
 * only one server writes is written over; one several servers write is written one change at a
 * time, each server making its own on the latest version, so nobody's change is ever undone.
 */
public interface ProgressionStore {

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
     * Writes each quest over what is stored, as one batch, for quests only this server writes. Who
     * holds a quest is read off the quest itself, so dropping a player from one unlinks them by
     * this call alone.
     */
    void saveProgressions(@Nonnull Collection<AbstractQuestProgression<?>> quests);

    /**
     * Writes a quest several servers write: reads the latest version, has {@code changes} make this
     * server's changes on it, and writes it only over that version, reading again and making them
     * again if another server wrote in between. Changes that make no difference write nothing.
     *
     * @param changes makes this server's changes on the stored copy it is handed and says whether
     * they changed it, which may happen more than once: the last copy handed over is the one kept
     * @return that copy, standing on the version it is stored at, or {@code null} if nothing is
     * stored under that id any more
     */
    @Nullable
    AbstractQuestProgression<?> commitProgression(@Nonnull UUID questId, @Nonnull Predicate<AbstractQuestProgression<?>> changes);

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
