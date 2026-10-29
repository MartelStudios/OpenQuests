package com.martelstudios.opendialogues.displays.impl.chat;

import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.displays.AnswerTints;
import com.martelstudios.opendialogues.displays.DialogueDisplay;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesPage;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesResponse;
import com.martelstudios.opendialogues.sessions.DialogueSession;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Draws a dialogue in the chat: its pages, then its answers numbered for {@code /oda}.
 */
public class ChatDisplay implements DialogueDisplay {

    public static final String ID = "Chat";

    @Override
    public void show(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        Color color = session.dialogue().getColor();
        if (session.dialogue().getTitle() != null) {
            player.sendMessage(tinted(Message.join(Message.raw("["), Message.translation(session.dialogue().getTitle()), Message.raw("]")).bold(true), color));
        }
        for (OpenDialoguesPage page : session.dialogue().getPages()) {
            player.sendMessage(tinted(Message.translation(page.getText()), color));
        }
        refresh(player, session);
    }

    /**
     * Prints the answers alone, below the pages the player already has in the chat.
     */
    @Override
    public void refresh(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        var responses = session.responses();
        for (int i = 0; i < responses.size(); i++) {
            OpenDialoguesResponse response = responses.get(i);
            player.sendMessage(Message.join(Message.raw("  " + (i + 1) + ". "), tinted(Message.translation(response.getText()), response.getColor())));
        }
        if (!responses.isEmpty()) {
            player.sendMessage(Message.raw("/oda <n>").italic(true));
        }
    }

    @Override
    public void close(@Nonnull PlayerRef player, @Nonnull DialogueSession session) {
        // The chat keeps what it printed: nothing to take down.
    }

    @Nonnull
    private static Message tinted(@Nonnull Message message, @Nullable Color color) {
        return color == null ? message : message.color(AnswerTints.base(color));
    }
}
