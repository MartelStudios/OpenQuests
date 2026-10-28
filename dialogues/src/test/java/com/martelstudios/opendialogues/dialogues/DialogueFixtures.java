package com.martelstudios.opendialogues.dialogues;

import com.martelstudios.opendialogues.CodecJson;

import javax.annotation.Nonnull;

/**
 * Dialogues as the asset store hands them out, id included.
 */
public final class DialogueFixtures {

    private DialogueFixtures() {}

    /**
     * @param body the dialogue file without its id.
     */
    @Nonnull
    public static OpenDialogueAsset dialogue(@Nonnull String id, @Nonnull String body) {
        OpenDialogueAsset dialogue = CodecJson.decode(OpenDialogueAsset.BODY_CODEC, body);
        if (dialogue == null) throw new IllegalArgumentException("Unreadable dialogue " + body);
        dialogue.id = id;
        return dialogue;
    }
}
