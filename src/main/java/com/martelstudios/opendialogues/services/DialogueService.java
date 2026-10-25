package com.martelstudios.opendialogues.services;

import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.api.DialogueContext;
import com.martelstudios.opendialogues.presenters.DialoguePresenter;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.events.DialogueEvent;
import com.martelstudios.opendialogues.sessions.DialogueSession;
import com.martelstudios.opendialogues.sessions.SessionStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Opens dialogues, runs the answers players pick and closes them, whatever draws them and however
 * the player answered. Open dialogues last for the session only.
 */
public class DialogueService {

    /**
     * What came of an answer.
     */
    public enum AnswerResult {
        ANSWERED,
        /** No dialogue open, or the one named is not open. */
        NO_DIALOGUE,
        /** Several dialogues open and none named. */
        AMBIGUOUS,
        NO_SUCH_ANSWER
    }

    private final SessionStore<DialogueSession> sessions = new SessionStore<>();

    @Nonnull
    private final DialogueRegistry registry;

    @Nonnull
    private final String defaultPresenter;

    public DialogueService(@Nonnull DialogueRegistry registry, @Nonnull String defaultPresenter) {
        this.registry = registry;
        this.defaultPresenter = defaultPresenter;
    }

    /**
     * Shows a dialogue to a player, replacing the same dialogue if it was open.
     *
     * @return whether a dialogue exists under that id.
     */
    public boolean open(@Nonnull PlayerRef player, @Nonnull String dialogueId) {
        OpenDialogueAsset dialogue = OpenDialogueAsset.getDialogue(dialogueId);
        if (dialogue == null) {
            return false;
        }
        open(player, dialogue);
        return true;
    }

    /**
     * Shows a dialogue to a player, one from the store or one written inline, replacing the same
     * dialogue if it was open.
     */
    public void open(@Nonnull PlayerRef player, @Nonnull OpenDialogueAsset dialogue) {
        // shortcut: every answer is shown, filter here once answers carry conditions.
        DialogueSession session = new DialogueSession(dialogue, List.of(dialogue.getResponses()), presenterFor(dialogue));
        sessions.put(player.getUuid(), dialogue.getId(), session);
        session.presenter().show(player, session);
        dispatch(new DialogueEvent.Opened(player, session), DialogueEvent.Opened.class);
    }

    /**
     * Runs the actions of an answer the player was shown.
     *
     * @param dialogueId the dialogue answered, or {@code null} when the player has a single one open.
     * @param position   the answer's position among those shown, from 1.
     */
    @Nonnull
    public AnswerResult answer(@Nonnull PlayerRef player, @Nullable String dialogueId, int position) {
        DialogueSession session = sessions.resolve(player.getUuid(), dialogueId);
        if (session == null) {
            return dialogueId == null && sessions.openIds(player.getUuid()).size() > 1 ? AnswerResult.AMBIGUOUS : AnswerResult.NO_DIALOGUE;
        }
        if (position < 1 || position > session.responses().size()) {
            return AnswerResult.NO_SUCH_ANSWER;
        }

        dispatch(new DialogueEvent.Answered(player, session, position), DialogueEvent.Answered.class);
        DialogueContext context = new DialogueContext(this, player, session);
        for (DialogueAction action : session.responses().get(position - 1).getActions()) {
            action.execute(context);
        }
        return AnswerResult.ANSWERED;
    }

    /**
     * Ends one of the player's dialogues.
     *
     * @return whether it was open.
     */
    public boolean close(@Nonnull PlayerRef player, @Nonnull String dialogueId) {
        DialogueSession session = sessions.remove(player.getUuid(), dialogueId);
        if (session == null) {
            return false;
        }
        closed(player, session);
        return true;
    }

    /**
     * Ends every dialogue of a player leaving, without drawing anything: there is no one to draw for.
     */
    public void forget(@Nonnull PlayerRef player) {
        for (DialogueSession session : sessions.removeAll(player.getUuid())) {
            dispatch(new DialogueEvent.Closed(player, session), DialogueEvent.Closed.class);
        }
    }

    /**
     * @return the ids of the player's open dialogues, in opening order.
     */
    @Nonnull
    public List<String> openDialogues(@Nonnull UUID player) {
        return sessions.openIds(player);
    }

    private void closed(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        session.presenter().close(player, session);
        dispatch(new DialogueEvent.Closed(player, session), DialogueEvent.Closed.class);
    }

    @Nonnull
    private DialoguePresenter presenterFor(@Nonnull OpenDialogueAsset dialogue) {
        DialoguePresenter presenter = dialogue.getPresenter() == null ? null : registry.getPresenter(dialogue.getPresenter());
        return presenter != null ? presenter : registry.getPresenter(defaultPresenter);
    }

    private static <E extends DialogueEvent> void dispatch(@Nonnull E event, @Nonnull Class<E> type) {
        HytaleServer.get().getEventBus().dispatchFor(type, event.getSession().dialogueId()).dispatch(event);
    }
}
