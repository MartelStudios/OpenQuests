package com.martelstudios.openquests.dialogues.actions;

import com.martelstudios.opendialogues.OpenDialoguesPlugin;
import com.martelstudios.opendialogues.api.DialogueRegistry;

/**
 * Answer actions acting on the player's quests.
 */
public final class QuestActionsFeature {

    private QuestActionsFeature() {}

    /**
     * Registers the actions with OpenDialogues, which sets up before this plugin.
     */
    public static void register() {
        DialogueRegistry registry = OpenDialoguesPlugin.get().getRegistry();
        registry.registerAction(OpenQuestsAssignQuestAction.TYPE, OpenQuestsAssignQuestAction.class, OpenQuestsAssignQuestAction.CODEC);
    }
}
