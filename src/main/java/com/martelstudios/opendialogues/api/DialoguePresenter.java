package com.martelstudios.opendialogues.api;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;

/**
 * Draws dialogues for players: the chat, a custom page, anything a mod registers. It only draws;
 * every answer comes back through {@code DialogueService.answer}, whatever the player clicked or typed.
 */
public interface DialoguePresenter {

    /**
     * Shows a dialogue just opened, or opened again.
     */
    void show(@Nonnull PlayerRef player, @Nonnull DialogueSession session);

    /**
     * Takes down a dialogue that ended.
     */
    void close(@Nonnull PlayerRef player, @Nonnull DialogueSession session);
}
