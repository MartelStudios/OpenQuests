package com.martelstudios.opendialogues.sessions;

import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesResponse;
import com.martelstudios.opendialogues.dialogues.SpeakerView;
import com.martelstudios.opendialogues.displays.DialogueDisplay;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * A dialogue open for one player. Keeps the answers as shown when it opened, so "answer 2" means
 * the second one the player saw even when hidden answers sit between them in the file.
 *
 * @param responses the answers shown, in the order shown.
 * @param source    the NPC the conversation was opened from, {@code null} for any other opening.
 * @param speaker   who to show speaking, the source NPC applied, {@code null} without a speaker.
 */
public record DialogueSession(@Nonnull OpenDialogueAsset dialogue, @Nonnull List<OpenDialoguesResponse> responses, @Nonnull DialogueDisplay display, @Nullable DialogueSource source, @Nullable SpeakerView speaker) {

    /**
     * @return the id of the open dialogue.
     */
    @Nonnull
    public String dialogueId() {
        return dialogue.getId();
    }

    /**
     * @return the character of the NPC the conversation was opened from, {@code null} without one.
     */
    @Nullable
    public String characterId() {
        return source == null ? null : source.character();
    }
}
