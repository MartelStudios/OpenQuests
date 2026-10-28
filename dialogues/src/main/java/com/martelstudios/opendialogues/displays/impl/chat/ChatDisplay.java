package com.martelstudios.opendialogues.displays.impl.chat;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.displays.DialogueDisplay;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesPage;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;

/**
 * Draws a dialogue in the chat: its pages, then its answers numbered for {@code /oda}.
 */
public class ChatDisplay implements DialogueDisplay {

    public static final String ID = "Chat";

    @Override
    public void show(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        if (session.dialogue().getTitle() != null) {
            player.sendMessage(Message.join(Message.raw("["), Message.translation(session.dialogue().getTitle()), Message.raw("]")).bold(true));
        }
        for (OpenDialoguesPage page : session.dialogue().getPages()) {
            player.sendMessage(Message.translation(page.getText()));
        }

        var responses = session.responses();
        for (int i = 0; i < responses.size(); i++) {
            player.sendMessage(Message.join(Message.raw("  " + (i + 1) + ". "), Message.translation(responses.get(i).getText())));
        }
        if (!responses.isEmpty()) {
            player.sendMessage(Message.raw("/oda <n>").italic(true));
        }
    }

    @Override
    public void close(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        // The chat keeps what it printed: nothing to take down.
    }
}
