package com.martelstudios.openquests.dialogues.givers;

import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The NPC that handed a quest out in dialogue, written as instance tags on the quest so it
 * outlives the conversation and reaches the steps of a composite through their head.
 *
 * @param character the id of the character the NPC carries.
 * @param npc       the NPC entity's UUID.
 * @param nameKey   the translation key of its displayed name, {@code null} for a nameless one.
 */
public record Giver(@Nonnull String character, @Nonnull UUID npc, @Nullable String nameKey) {

    public static final String CHARACTER_TAG = "OpenQuestsDialogues.GiverCharacter";
    public static final String NPC_TAG = "OpenQuestsDialogues.GiverNpc";
    /** Read by the quest texts as the {@code {giver}} parameter. */
    public static final String NAME_TAG = "Text.giver";

    /**
     * @return the NPC a conversation was opened from, {@code null} for one opened without any.
     */
    @Nullable
    public static Giver of(@Nullable DialogueSource source) {
        if (source == null) return null;

        return new Giver(source.character(), source.npc(), source.name() != null ? source.name() : source.epithet());
    }

    /**
     * @return the giver written on the quest's head, {@code null} for a quest no NPC handed out.
     */
    @Nullable
    public static Giver of(@Nonnull AbstractQuestProgression<?> quest) {
        var tags = quest.getHead().getTags();
        String character = first(tags.get(CHARACTER_TAG));
        String npc = first(tags.get(NPC_TAG));
        if (character == null || npc == null) return null;

        try {
            return new Giver(character, UUID.fromString(npc), first(tags.get(NAME_TAG)));
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    /**
     * Writes the giver on a quest nobody holds yet, so its tags exist before any listener hears of it.
     */
    public void writeOn(@Nonnull AbstractQuestProgression<?> quest) {
        quest.apply(new AbstractQuestProgression.PutTag(CHARACTER_TAG, new String[] {character}));
        quest.apply(new AbstractQuestProgression.PutTag(NPC_TAG, new String[] {npc.toString()}));
        if (nameKey != null) quest.apply(new AbstractQuestProgression.PutTag(NAME_TAG, new String[] {nameKey}));
    }

    @Nullable
    private static String first(@Nullable String[] values) {
        return values == null || values.length == 0 ? null : values[0];
    }
}
