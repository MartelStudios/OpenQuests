package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The progressions. Each server writes its own replica of a quest and nothing else of it, so
 * servers sharing a quest never overwrite one another: a quest read back is every replica merged.
 * Its end is the one thing written once for all, by whichever server claims it first.
 */
public interface ProgressionStore {

    /**
     * @return the quest under that id, every replica merged and its claimed end applied, or
     * {@code null} for one nothing answers to.
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
     * Writes this server's replica of each quest as one batch. Who holds a quest is read off the
     * quest itself, so dropping a player from one unlinks them by this call alone.
     */
    void saveProgressions(@Nonnull Collection<AbstractQuestProgression<?>> quests);

    /**
     * Does away with a quest, every replica and every link to it. Takes the quest rather than its
     * id: a backend may need the players it named, and nothing else still knows who they were.
     *
     * <p>A backend several servers share keeps an ended quest's outcome a while longer: a server
     * still running the quest learns it ended, rather than ending it a second time.
     */
    void deleteProgression(@Nonnull AbstractQuestProgression<?> quest);

    /**
     * Ends a quest for every server at once, with the outcome and moment the quest carries.
     *
     * @return {@code null} if this server's outcome is the one written, otherwise the outcome
     * another server claimed first.
     */
    @Nullable
    QuestState claimEnd(@Nonnull AbstractQuestProgression<?> quest);

    /**
     * What other servers wrote of these quests since what was last seen of them: the replicas
     * newer than the revisions known, and the outcomes claimed meanwhile.
     *
     * @param known for each quest, the revision last seen of each other server's replica
     */
    @Nonnull
    ReplicaPoll pollReplicas(@Nonnull Map<UUID, Map<String, Long>> known);
}
