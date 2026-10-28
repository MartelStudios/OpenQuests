package com.martelstudios.openquests.dialogues.quests.dialogue;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Open a dialogue, or pick one of its answers, a number of times.
 */
public final class DialogueFeature {
    public static final String TYPE_ID = "Dialogue";

    private DialogueFeature() {}

    /**
     * Registers the quest type.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, OpenQuestsDialogueAsset.class, OpenQuestsDialogueAsset.CODEC, DialogueQuestProgression.class, DialogueQuestProgression.CODEC);
    }
}
