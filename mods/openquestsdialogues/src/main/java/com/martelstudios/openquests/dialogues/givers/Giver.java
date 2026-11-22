package com.martelstudios.openquests.dialogues.givers;

import com.martelstudios.opendialogues.characters.runtime.ReleasedNpcs;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.function.Function;

/**
 * The NPC that handed a quest out in dialogue, written as instance tags on the quest so it
 * outlives the conversation and reaches the steps of a composite through their head.
 *
 * @param character the id of the character the NPC carries.
 * @param npc       the NPC entity's UUID.
 * @param nameKey   the translation key of its displayed name, {@code null} for a nameless one.
 * @param since     when the NPC was last known carrying that character, {@code null} for unknown.
 */
public record Giver(@Nonnull String character, @Nonnull UUID npc, @Nullable String nameKey, @Nullable Instant since) {

    public static final String CHARACTER_TAG = "OpenQuestsDialogues.GiverCharacter";
    public static final String NPC_TAG = "OpenQuestsDialogues.GiverNpc";
    public static final String SINCE_TAG = "OpenQuestsDialogues.GiverSince";
    /** Read by the quest texts as the {@code {giver}} parameter. */
    public static final String NAME_TAG = "Text.giver";

    /**
     * A giver with no known date, released whenever its NPC is.
     */
    public Giver(@Nonnull String character, @Nonnull UUID npc, @Nullable String nameKey) {
        this(character, npc, nameKey, null);
    }

    /**
     * @return the NPC a conversation was opened from, {@code null} for one opened without any.
     */
    @Nullable
    public static Giver of(@Nullable DialogueSource source) {
        if (source == null) return null;

        return new Giver(source.character(), source.npc(), source.name() != null ? source.name() : source.epithet(), source.openedAt().truncatedTo(ChronoUnit.MILLIS));
    }

    /**
     * A quest written before the date tag existed falls back on when its head started.
     *
     * @return the giver written on the quest's head, {@code null} for a quest no NPC handed out.
     */
    @Nullable
    public static Giver of(@Nonnull AbstractQuestProgression<?> quest) {
        AbstractQuestProgression<?> head = quest.getHead();
        var tags = head.getTags();
        String character = first(tags.get(CHARACTER_TAG));
        String npc = first(tags.get(NPC_TAG));
        if (character == null || npc == null) return null;

        try {
            return new Giver(character, UUID.fromString(npc), first(tags.get(NAME_TAG)), since(first(tags.get(SINCE_TAG)), head.getStartedAt()));
        } catch (IllegalArgumentException malformed) {
            return null;
        }
    }

    /**
     * @param releasedAt when each NPC last let its character go, OpenDialogues' {@code CharacterService.releasedAt}.
     * @return whether the NPC let this character go after handing the quest out, even if it carries one again.
     */
    public boolean isReleased(@Nonnull Function<UUID, Instant> releasedAt) {
        return ReleasedNpcs.releasedSince(releasedAt.apply(npc), since);
    }

    /**
     * Writes the giver on a quest nobody holds yet, so its tags exist before any listener hears of it.
     */
    public void writeOn(@Nonnull AbstractQuestProgression<?> quest) {
        quest.apply(new AbstractQuestProgression.PutTag(CHARACTER_TAG, new String[] {character}));
        quest.apply(new AbstractQuestProgression.PutTag(NPC_TAG, new String[] {npc.toString()}));
        if (since != null) quest.apply(new AbstractQuestProgression.PutTag(SINCE_TAG, new String[] {Long.toString(since.toEpochMilli())}));
        if (nameKey != null) quest.apply(new AbstractQuestProgression.PutTag(NAME_TAG, new String[] {nameKey}));
    }

    @Nullable
    private static Instant since(@Nullable String millis, @Nullable Instant startedAt) {
        if (millis == null) return startedAt;
        try {
            return Instant.ofEpochMilli(Long.parseLong(millis));
        } catch (NumberFormatException malformed) {
            return startedAt;
        }
    }

    @Nullable
    private static String first(@Nullable String[] values) {
        return values == null || values.length == 0 ? null : values[0];
    }
}
