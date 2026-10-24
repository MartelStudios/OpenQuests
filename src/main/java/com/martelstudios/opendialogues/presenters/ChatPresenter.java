package com.martelstudios.opendialogues.presenters;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.api.DialoguePresenter;
import com.martelstudios.opendialogues.dialogues.DialoguePage;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Draws a dialogue in the chat: its pages, then its answers numbered for {@code /oda}. Several
 * dialogues can share the chat, so the answers end with the command naming their dialogue.
 */
public class ChatPresenter implements DialoguePresenter {

    public static final String ID = "OpenDialoguesChat";

    @Override
    public void show(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        if (session.dialogue().getTitle() != null) {
            player.sendMessage(Message.raw("[" + session.dialogue().getTitle() + "]").bold(true));
        }
        for (DialoguePage page : session.dialogue().getPages()) {
            player.sendMessage(Message.raw(page.getText()));
        }

        var responses = session.responses();
        for (int i = 0; i < responses.size(); i++) {
            player.sendMessage(Message.raw("  " + (i + 1) + ". " + responses.get(i).getText()));
        }
        if (!responses.isEmpty()) {
            player.sendMessage(Message.raw("/oda <n> " + session.dialogueId()).italic(true));
        }
    }

    @Override
    public void close(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        // The chat keeps what it printed: nothing to take down.
    }

    /**
     * @return the line telling the player which dialogues an answer could be for.
     */
    @Nonnull
    public static Message ambiguous(@Nonnull List<String> openDialogues) {
        return Message.raw("Several dialogues are open, name one: /oda <n> <" + String.join("|", openDialogues) + ">");
    }
}
