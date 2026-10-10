package com.martelstudios.openquests.core.models;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * One thing that happened to a quest, as data: made on the copy where it happened, then made again
 * on any other copy of the quest, which comes out the same. Every change to a quest is one.
 *
 * <p>A quest type declares the operations it understands next to its fields, as records, and its
 * subtypes inherit them; these are the ones every quest understands. One that moves the quest on
 * refuses a quest that is over, which a copy ended elsewhere may be.
 *
 * @param <Q> the quests this operation can be made on
 */
public interface QuestOperation<Q extends AbstractQuestProgression<?>> {

    /**
     * Reads nothing but the quest it is handed and its own fields, so that it comes out the same
     * on every copy. Tells nobody: the quest does, once the operation is made.
     *
     * @return whether it changed the quest.
     */
    boolean applyTo(@Nonnull Q quest);

    /**
     * @return one operation doing what this one then that one do, for two kept between the same
     * writes, or {@code null} for two that stay apart, which most do.
     */
    @Nullable
    default QuestOperation<Q> followedBy(@Nonnull QuestOperation<?> next) {
        return null;
    }

    /**
     * Ends the quest on that state, or brings one kept running by {@code StopOnComplete: false}
     * back. A quest that is over takes no more.
     */
    record SetState(@Nonnull QuestState state) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            if (quest.isOver() || quest.state == state) return false;

            quest.state = state;
            return true;
        }
    }

    /**
     * Hands the quest to a player, one who walked away from it included: they run it again.
     */
    record Join(@Nonnull UUID playerId) implements QuestOperation<AbstractQuestProgression<?>> {
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
    record Leave(@Nonnull UUID playerId) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.players.remove(playerId);
        }
    }

    /**
     * A player running the quest gives it up.
     */
    record Abandon(@Nonnull UUID playerId) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            if (!quest.players.remove(playerId)) return false;

            quest.abandonedPlayers.add(playerId);
            return true;
        }
    }

    /**
     * @param track {@code null} hands the answer back to the asset
     */
    record Track(@Nullable Boolean track) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            if (Objects.equals(quest.track, track)) return false;

            quest.track = track;
            return true;
        }
    }

    /**
     * Writes a tag and what it carries, replacing whatever the quest held under it.
     */
    record PutTag(@Nonnull String tag, @Nonnull String[] values) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return !Arrays.equals(quest.tags.put(tag, values), values);
        }
    }

    record RemoveTag(@Nonnull String tag) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.tags.remove(tag) != null;
        }
    }

    /**
     * Written by the scope holding the quest, never by the quest itself.
     */
    record SetScope(@Nullable QuestScope scope) implements QuestOperation<AbstractQuestProgression<?>> {
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
    record AddWorld(@Nonnull UUID worldId) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.scope != null && quest.scope.addWorld(worldId);
        }
    }

    /**
     * A world of the scope holding the quest closed.
     */
    record RemoveWorld(@Nonnull UUID worldId) implements QuestOperation<AbstractQuestProgression<?>> {
        @Override
        public boolean applyTo(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.scope != null && quest.scope.removeWorld(worldId);
        }
    }
}
