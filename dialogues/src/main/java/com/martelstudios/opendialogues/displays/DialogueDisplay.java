package com.martelstudios.opendialogues.displays;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;

/**
 * Draws dialogues for players: the chat, a custom page, anything a mod registers. It only draws;
 * every answer comes back through {@code DialogueService.answer}, whatever the player clicked or typed.
 */
public interface DialogueDisplay {

    /**
     * Shows a dialogue just opened, or opened again. Without answers it has already ended: a
     * blocking display stays up until the player closes it, and {@link #close} is not called.
     */
    void show(@Nonnull PlayerRef player, @Nonnull DialogueSession session);

    /**
     * Shows the answers of the dialogue on screen again, after they changed under the player. A
     * display that cannot redraw them alone draws the whole dialogue anew.
     */
    default void refresh(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        show(player, session);
    }

    /**
     * Takes down a dialogue that ended.
     */
    void close(@Nonnull PlayerRef player, @Nonnull DialogueSession session);
}
