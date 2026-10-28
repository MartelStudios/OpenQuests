package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.opendialogues.events.DialogueEvent;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;

import javax.annotation.Nonnull;

/**
 * Open a dialogue, or pick one of its answers, a number of times.
 */
public final class DialogueFeature {
    public static final String TYPE_ID = "Dialogue";

    private DialogueFeature() {}

    /**
     * Registers the quest type and the dialogue events feeding it.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, OpenQuestsDialogueAsset.class, OpenQuestsDialogueAsset.CODEC, DialogueQuestProgression.class, DialogueQuestProgression.CODEC);

        DialogueQuestEvents events = new DialogueQuestEvents(services.getQuestProgressionService());
        plugin.getEventRegistry().registerGlobal(DialogueEvent.Opened.class, events::onOpened);
        plugin.getEventRegistry().registerGlobal(DialogueEvent.Answered.class, events::onAnswered);
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, events::onQuestAssigned);
    }
}
