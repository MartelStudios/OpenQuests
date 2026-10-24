package com.martelstudios.opendialogues.services;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.api.DialogueAction;
import com.martelstudios.opendialogues.dialogues.DialoguePage;
import com.martelstudios.opendialogues.dialogues.DialogueResponse;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opens dialogues for players and runs the answers they pick. Remembers only which dialogue each player
 * stands in, for this session.
 */
public class DialogueService {

    private final Map<UUID, String> openDialogues = new ConcurrentHashMap<>();

    /**
     * Shows a dialogue to a player, replacing any dialogue they stood in.
     *
     * @return whether a dialogue exists under that id.
     */
    public boolean open(@Nonnull PlayerRef player, @Nonnull String dialogueId) {
        OpenDialogueAsset dialogue = OpenDialogueAsset.getDialogue(dialogueId);
        if (dialogue == null) {
            player.sendMessage(Message.raw("No dialogue '" + dialogueId + "'."));
            return false;
        }

        openDialogues.put(player.getUuid(), dialogueId);

        // shortcut: chat output stands in for the dialogue interface, replace once the UI task lands.
        if (dialogue.getTitle() != null) {
            player.sendMessage(Message.raw("[" + dialogue.getTitle() + "]"));
        }
        for (DialoguePage page : dialogue.getPages()) {
            player.sendMessage(Message.raw(page.getText()));
        }
        DialogueResponse[] responses = dialogue.getResponses();
        for (int i = 0; i < responses.length; i++) {
            player.sendMessage(Message.raw((i + 1) + ". " + responses[i].getText()));
        }
        return true;
    }

    /**
     * Runs the actions of the answer at that position, counted from 1, in the player's dialogue.
     *
     * @return whether the player stood in a dialogue offering that answer.
     */
    public boolean answer(@Nonnull PlayerRef player, int position) {
        String dialogueId = openDialogues.get(player.getUuid());
        OpenDialogueAsset dialogue = dialogueId == null ? null : OpenDialogueAsset.getDialogue(dialogueId);
        if (dialogue == null || position < 1 || position > dialogue.getResponses().length) {
            return false;
        }

        for (DialogueAction action : dialogue.getResponses()[position - 1].getActions()) {
            action.execute(this, player);
        }
        return true;
    }

    /**
     * Ends the player's dialogue, if any.
     */
    public void close(@Nonnull PlayerRef player) {
        if (openDialogues.remove(player.getUuid()) != null) {
            player.sendMessage(Message.raw("[Dialogue closed]"));
        }
    }
}
