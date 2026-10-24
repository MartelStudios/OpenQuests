package com.martelstudios.opendialogues.sessions;

import com.martelstudios.opendialogues.presenters.DialoguePresenter;
import com.martelstudios.opendialogues.dialogues.DialogueResponse;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A dialogue open for one player. Keeps the answers as shown when it opened, so "answer 2" means
 * the second one the player saw even when hidden answers sit between them in the file.
 *
 * @param responses the answers shown, in the order shown.
 */
public record DialogueSession(@Nonnull OpenDialogueAsset dialogue, @Nonnull List<DialogueResponse> responses, @Nonnull DialoguePresenter presenter) {

    /**
     * @return the id of the open dialogue.
     */
    @Nonnull
    public String dialogueId() {
        return dialogue.getId();
    }
}
