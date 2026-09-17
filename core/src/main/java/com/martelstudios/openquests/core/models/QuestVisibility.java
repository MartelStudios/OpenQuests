package com.martelstudios.openquests.core.models;

/**
 * When a quest is worth putting in front of the player. Says nothing about whether it announces
 * how it ended, which is {@code AnnounceOutcome}, nor about what it owes them: a debt is listed in
 * its own right, so a quest nobody ever sees still pays out.
 */
public enum QuestVisibility {
    /**
     * Listed from the moment it is handed out.
     */
    ALWAYS,

    /**
     * Listed once the player has got somewhere with it, which keeps a catalogue handed out on
     * connection from filling the panel with quests nobody has started.
     */
    WHEN_PROGRESSED,

    /**
     * Listed only once it is over — the shape of an achievement, earned before it is named.
     */
    WHEN_COMPLETED,

    /**
     * Never listed, whatever becomes of it.
     */
    NEVER
}
