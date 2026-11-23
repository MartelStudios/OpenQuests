package com.martelstudios.opendialogues.conditions;

import com.martelstudios.opendialogues.sessions.DialogueSource;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Whose dialogues or quests a condition counts, relative to the NPC the conversation comes from.
 */
public enum SourceScope {
    /**
     * Every NPC, the source ignored.
     */
    Any,
    /**
     * Every NPC carrying the source's character.
     */
    Character,
    /**
     * The source NPC alone.
     */
    Npc;

    /**
     * @return the character to filter on, {@code null} for any.
     */
    @Nullable
    public String character(@Nullable DialogueSource source) {
        return this == Any || source == null ? null : source.character();
    }

    /**
     * @return the NPC to filter on, {@code null} for any.
     */
    @Nullable
    public UUID npc(@Nullable DialogueSource source) {
        return this == Npc && source != null ? source.npc() : null;
    }

    /**
     * @return whether the scope needs a source and there is none, so nothing can count.
     */
    public boolean blind(@Nullable DialogueSource source) {
        return this != Any && source == null;
    }
}
