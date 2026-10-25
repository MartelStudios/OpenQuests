package com.martelstudios.opendialogues;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorFetchAutoCompleteDataEvent;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.martelstudios.opendialogues.actions.impl.close.CloseDialogueAction;
import com.martelstudios.opendialogues.actions.impl.command.CommandDialogueAction;
import com.martelstudios.opendialogues.actions.impl.open.OpenDialogueAction;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.commands.AnswerCommand;
import com.martelstudios.opendialogues.commands.DialogueCommand;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialogueSpeakerAsset;
import com.martelstudios.opendialogues.displays.DisplayRules;
import com.martelstudios.opendialogues.displays.impl.chat.ChatDisplay;
import com.martelstudios.opendialogues.displays.impl.page.PageDisplay;
import com.martelstudios.opendialogues.services.DialogueService;


import javax.annotation.Nonnull;
import java.util.Locale;

/**
 * Dialogues and their speakers as assets: written in the Asset Editor, opened by id and drawn by
 * whichever display they name.
 */
public class OpenDialoguesPlugin extends JavaPlugin {

    private static OpenDialoguesPlugin instance;

    private final DialogueRegistry registry = new DialogueRegistry();

    private final Config<DisplayRules> displayRules;

    private DialogueService dialogueService;

    public OpenDialoguesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        displayRules = withConfig("Displays", DisplayRules.CODEC);
    }

    public static OpenDialoguesPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        registry.registerAction(CloseDialogueAction.TYPE, CloseDialogueAction.class, CloseDialogueAction.CODEC);
        registry.registerAction(OpenDialogueAction.TYPE, OpenDialogueAction.class, OpenDialogueAction.CODEC);
        registry.registerAction(CommandDialogueAction.TYPE, CommandDialogueAction.class, CommandDialogueAction.CODEC);

        dialogueService = new DialogueService(registry, displayRules::get, ChatDisplay.ID);
        registry.registerDisplay(ChatDisplay.ID, new ChatDisplay());
        registry.registerDisplay(PageDisplay.ID, new PageDisplay(dialogueService));

        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogueAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogues/Dialogues/")
                                                    .setCodec(OpenDialogueAsset.CODEC)
                                                    .setKeyFunction(OpenDialogueAsset::getId)
                                                    .build());
        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogueSpeakerAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogues/Speakers/")
                                                    .setCodec(OpenDialogueSpeakerAsset.CODEC)
                                                    .setKeyFunction(OpenDialogueSpeakerAsset::getId)
                                                    .build());

        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> dialogueService.forget(event.getPlayerRef()));
        getEventRegistry().register(AssetEditorFetchAutoCompleteDataEvent.class, DialogueRegistry.DISPLAYS_DATA_SET, event -> {
            String query = event.getQuery().toLowerCase(Locale.ROOT);
            event.setResults(registry.getDisplayIds().stream().filter(id -> id.toLowerCase(Locale.ROOT).contains(query)).sorted().toArray(String[]::new));
        });

        getCommandRegistry().registerCommand(new DialogueCommand(dialogueService, registry, displayRules));
        getCommandRegistry().registerCommand(new AnswerCommand("oda", dialogueService));
    }

    /**
     * @return where actions and displays are registered.
     */
    @Nonnull
    public DialogueRegistry getRegistry() {
        return registry;
    }

    @Nonnull
    public DialogueService getDialogueService() {
        return dialogueService;
    }
}
