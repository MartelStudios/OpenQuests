package com.martelstudios.opendialogs.services;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogs.actions.DialogAction;
import com.martelstudios.opendialogs.dialogs.DialogPage;
import com.martelstudios.opendialogs.dialogs.DialogResponse;
import com.martelstudios.opendialogs.dialogs.OpenDialogsDialog;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens dialogs for players and runs the answers they pick. Remembers only which dialog each player
 * stands in, for this session.
 */
public class DialogService {

    private final Map<UUID, String> openDialogs = new ConcurrentHashMap<>();

    /**
     * Shows a dialog to a player, replacing any dialog they stood in.
     *
     * @return whether a dialog exists under that id.
     */
    public boolean open(@Nonnull PlayerRef player, @Nonnull String dialogId) {
        OpenDialogsDialog dialog = OpenDialogsDialog.getDialog(dialogId);
        if (dialog == null) {
            player.sendMessage(Message.raw("No dialog '" + dialogId + "'."));
            return false;
        }

        openDialogs.put(player.getUuid(), dialogId);

        // shortcut: chat output stands in for the dialog interface, replace once the UI task lands.
        if (dialog.getTitle() != null) {
            player.sendMessage(Message.raw("[" + dialog.getTitle() + "]"));
        }
        for (DialogPage page : dialog.getPages()) {
            player.sendMessage(Message.raw(page.getText()));
        }
        DialogResponse[] responses = dialog.getResponses();
        for (int i = 0; i < responses.length; i++) {
            player.sendMessage(Message.raw((i + 1) + ". " + responses[i].getText()));
        }
        return true;
    }

    /**
     * Runs the actions of the answer at that position, counted from 1, in the player's dialog.
     *
     * @return whether the player stood in a dialog offering that answer.
     */
    public boolean answer(@Nonnull PlayerRef player, int position) {
        String dialogId = openDialogs.get(player.getUuid());
        OpenDialogsDialog dialog = dialogId == null ? null : OpenDialogsDialog.getDialog(dialogId);
        if (dialog == null || position < 1 || position > dialog.getResponses().length) {
            return false;
        }

        for (DialogAction action : dialog.getResponses()[position - 1].getActions()) {
            action.execute(this, player);
        }
        return true;
    }

    /**
     * Ends the player's dialog, if any.
     */
    public void close(@Nonnull PlayerRef player) {
        if (openDialogs.remove(player.getUuid()) != null) {
            player.sendMessage(Message.raw("[Dialog closed]"));
        }
    }
}
