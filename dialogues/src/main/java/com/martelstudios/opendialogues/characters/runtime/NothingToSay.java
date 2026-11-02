package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.actions.impl.close.OpenDialoguesCloseAction;
import com.martelstudios.opendialogues.api.PlayerTexts;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialoguesResponse;
import com.martelstudios.opendialogues.sessions.DialogueSource;
import com.martelstudios.opendialogues.triggers.DialogueTriggers;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * What F shows on a character only another mod made answer it, when that mod had nothing for this
 * player: a polite word rather than a press that seems broken.
 */
public final class NothingToSay {

    public static final String DIALOGUE_ID = "OpenDialogues_NothingToSay";

    private NothingToSay() {}

    /**
     * Opens "{name} has nothing to tell you." with a single goodbye, the NPC as source.
     */
    public static void open(@Nonnull CharacterContext context, @Nonnull PlayerRef player) {
        String nameKey = context.identity().getName();
        String epithet = context.character().getEpithet();
        String name = nameKey != null ? PlayerTexts.translate(player, nameKey, Map.of())
                      : epithet != null ? PlayerTexts.translate(player, epithet, Map.of())
                      : context.character().getId();

        OpenDialogueAsset dialogue = OpenDialogueAsset.generated(DIALOGUE_ID, OpenDialogueAsset.NPC_SPEAKER,
            new String[]{PlayerTexts.translate(player, "opendialogues.character.nothing", Map.of("name", name))},
            new OpenDialoguesResponse[]{OpenDialoguesResponse.generated("Goodbye", PlayerTexts.translate(player, "opendialogues.character.goodbye", Map.of()), new OpenDialoguesCloseAction())});
        DialogueTriggers.openFor(player, dialogue, new DialogueSource(context.uuid(), context.character().getId(), nameKey, null));
    }
}
