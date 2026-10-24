package com.martelstudios.opendialogues.events;

import com.hypixel.hytale.event.IEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;

/**
 * Something that happened to a player's dialogue. Keyed on the dialogue id, so a listener can hear
 * one dialogue only, a quest objective "have this discussion" for instance.
 * <p>
 * For reactions, not for drawing: drawing is the presenter's.
 */
public abstract class DialogueEvent implements IEvent<String> {

    @Nonnull
    private final PlayerRef player;

    @Nonnull
    private final DialogueSession session;

    protected DialogueEvent(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        this.player = player;
        this.session = session;
    }

    /**
     * @return the player the dialogue belongs to.
     */
    @Nonnull
    public PlayerRef getPlayer() {
        return player;
    }

    /**
     * @return the dialogue, as open for that player.
     */
    @Nonnull
    public DialogueSession getSession() {
        return session;
    }

    /**
     * Fired once a dialogue is open and shown.
     */
    public static class Opened extends DialogueEvent {
        public Opened(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
            super(player, session);
        }
    }

    /**
     * Fired when the player picks an answer, before its actions run.
     */
    public static class Answered extends DialogueEvent {

        private final int position;

        public Answered(@Nonnull PlayerRef player, @Nonnull DialogueSession session, int position) {
            super(player, session);
            this.position = position;
        }

        /**
         * @return the position of the answer among those shown, from 1.
         */
        public int getPosition() {
            return position;
        }
    }

    /**
     * Fired once a dialogue is closed, by an answer, another dialogue replacing it, or the player leaving.
     */
    public static class Closed extends DialogueEvent {
        public Closed(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
            super(player, session);
        }
    }
}
