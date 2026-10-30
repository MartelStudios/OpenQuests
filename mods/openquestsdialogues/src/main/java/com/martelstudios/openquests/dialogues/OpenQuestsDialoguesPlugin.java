package com.martelstudios.openquests.dialogues;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.dialogues.actions.QuestActionsFeature;
import com.martelstudios.openquests.dialogues.quests.dialogue.DialogueFeature;

import javax.annotation.Nonnull;

/**
 * Bridge between OpenQuests and OpenDialogues, so neither mod has to know the other. Each piece is a
 * feature registering itself, so this class only lists them.
 */
public class OpenQuestsDialoguesPlugin extends JavaPlugin {

    public OpenQuestsDialoguesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        // Set up by OpenQuestsCore, which this plugin depends on and which sets up first
        OpenQuestsServices services = OpenQuestsServices.installed();

        DialogueFeature.register(this, services);
        QuestActionsFeature.register();
    }
}
