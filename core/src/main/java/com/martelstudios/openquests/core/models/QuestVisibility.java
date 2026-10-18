package com.martelstudios.openquests.core.models;

import javax.annotation.Nullable;

/**
 * When a quest, or a chapter of quests, is worth putting in front of the player. Says nothing about
 * whether a quest announces how it ended, which is {@code AnnounceOutcome}, nor about what it owes
 * them: a debt is listed in its own right, so a quest nobody ever sees still pays out.
 */
public enum QuestVisibility {
    /**
     * Listed even before it is handed out, wherever a list can show that: a chapter shows the
     * quest locked, and a chapter itself shows from the start.
     */
    ALWAYS,

    /**
     * Listed from the moment it is handed out. The same as {@link #ALWAYS} for the quests a player
     * holds; it hides a quest from its chapter until then, and a chapter until one of its quests is.
     */
    WHEN_STARTED,

    /**
     * Listed once the player has got somewhere with it, which keeps a catalogue handed out on
     * connection from filling the panel with quests nobody has started.
     */
    WHEN_PROGRESSED,

    /**
     * Listed only once it is over, however it ended: the shape of an achievement, earned before it
     * is named.
     */
    WHEN_COMPLETED,

    /**
     * Listed only once it succeeded, so a quest failed or given up stays out of sight.
     */
    WHEN_SUCCESSFUL,

    /**
     * Never listed, whatever becomes of it.
     */
    NEVER;

    /**
     * @param quest the player's run of the quest, or {@code null} for one they were never handed.
     * @return whether that run, or the lack of one, is enough to list what this visibility guards.
     * A chapter asks it of each of its quests and shows once one answers yes.
     */
    public boolean isReachedBy(@Nullable AbstractQuestProgression<?> quest) {
        if (this == ALWAYS) return true;
        if (quest == null) return false;

        return switch (this) {
            case ALWAYS, WHEN_STARTED -> true;
            case WHEN_PROGRESSED -> quest.hasProgressed();
            case WHEN_COMPLETED -> quest.isCompleted();
            case WHEN_SUCCESSFUL -> quest.isSuccessful();
            case NEVER -> false;
        };
    }
}
