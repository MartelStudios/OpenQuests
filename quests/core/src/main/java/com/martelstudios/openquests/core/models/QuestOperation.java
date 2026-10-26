package com.martelstudios.openquests.core.models;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One thing that happened to a quest, as data: made on the copy where it happened, then made again
 * on any other copy of the quest, which comes out the same. Every change to a quest is one.
 *
 * <p>A quest type declares the operations it understands next to its fields, as records nested in
 * it, and its subtypes inherit them: {@link AbstractQuestProgression} declares those every quest
 * understands. One that moves the quest on refuses a quest that is over, which a copy ended
 * elsewhere may be.
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
}
