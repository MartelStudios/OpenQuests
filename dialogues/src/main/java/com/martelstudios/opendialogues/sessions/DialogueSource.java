package com.martelstudios.opendialogues.sessions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The NPC a conversation was opened from, carried along the dialogues its answers open.
 *
 * @param npc       the NPC entity's UUID.
 * @param character the id of the character it carries.
 * @param name      the translation key of its name, {@code null} for a nameless one.
 * @param speaker   the id of the speaker whose set fields the NPC lends, {@code null} for none.
 */
public record DialogueSource(@Nonnull UUID npc, @Nonnull String character, @Nullable String name, @Nullable String speaker) {}
