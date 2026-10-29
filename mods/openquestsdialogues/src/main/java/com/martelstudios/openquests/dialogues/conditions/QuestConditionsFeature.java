package com.martelstudios.openquests.dialogues.conditions;

import com.martelstudios.opendialogues.OpenDialoguesPlugin;

/**
 * Dialogue conditions judging the player's quests.
 */
public final class QuestConditionsFeature {

    private QuestConditionsFeature() {}

    /**
     * Registers the conditions with OpenDialogues, which sets up before this plugin.
     */
    public static void register() {
        OpenDialoguesPlugin.get().getRegistry().registerCondition(OpenQuestsRequiresQuestCondition.TYPE, OpenQuestsRequiresQuestCondition.class, OpenQuestsRequiresQuestCondition.CODEC);
    }
}
