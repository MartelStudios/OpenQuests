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
import com.martelstudios.opendialogues.events.DialogueEvent;
import com.martelstudios.opendialogues.routers.OpenDialogueRouterAsset;
import com.martelstudios.opendialogues.routers.impl.sequence.SequenceSelector;
import com.martelstudios.opendialogues.routers.impl.weighted.WeightedSelector;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.history.DialogueHistoryRecorder;
import com.martelstudios.opendialogues.displays.impl.chat.ChatDisplay;
import com.martelstudios.opendialogues.displays.impl.page.PageDisplay;
import com.martelstudios.opendialogues.services.DialogueService;


import com.martelstudios.opendialogues.conditions.impl.history.AnsweredCondition;
import com.martelstudios.opendialogues.conditions.impl.history.SeenCondition;
import com.martelstudios.opendialogues.conditions.impl.items.HoldingCondition;
import com.martelstudios.opendialogues.conditions.impl.items.InventoryCondition;
import com.martelstudios.opendialogues.conditions.impl.logic.CompositeCondition;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.martelstudios.opendialogues.services.DialogueResolver;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Dialogues and their speakers as assets: written in the Asset Editor, opened by id and drawn by
 * whichever display they name.
 */
public class OpenDialoguesPlugin extends JavaPlugin {

    private static OpenDialoguesPlugin instance;

    private final DialogueRegistry registry = new DialogueRegistry();

    private final Config<DisplayRules> displayRules;

    private DialogueService dialogueService;

    private DialogueHistoryRecorder historyRecorder;

    private DialogueResolver resolver;

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

        registry.registerCondition(CompositeCondition.TYPE, CompositeCondition.class, CompositeCondition.CODEC);
        registry.registerCondition(SeenCondition.TYPE, SeenCondition.class, SeenCondition.CODEC);
        registry.registerCondition(AnsweredCondition.TYPE, AnsweredCondition.class, AnsweredCondition.CODEC);
        registry.registerCondition(HoldingCondition.TYPE, HoldingCondition.class, HoldingCondition.CODEC);
        registry.registerCondition(InventoryCondition.TYPE, InventoryCondition.class, InventoryCondition.CODEC);

        registry.registerSelector(SequenceSelector.TYPE, SequenceSelector.class, SequenceSelector.CODEC);
        registry.registerSelector(WeightedSelector.TYPE, WeightedSelector.class, WeightedSelector.CODEC);

        historyRecorder = new DialogueHistoryRecorder(getEntityStoreRegistry().registerComponent(DialogueHistory.class, "OpenDialoguesHistory", DialogueHistory.CODEC));
        resolver = new DialogueResolver(OpenDialogueAsset::getDialogue, OpenDialogueRouterAsset::getRouter, new SplittableRandom());
        dialogueService = new DialogueService(registry, displayRules::get, ChatDisplay.ID, resolver, historyRecorder::history);
        registry.registerDisplay(ChatDisplay.ID, new ChatDisplay());
        registry.registerDisplay(PageDisplay.ID, new PageDisplay(dialogueService));

        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogueAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogues/Dialogues/")
                                                    .setCodec(OpenDialogueAsset.CODEC)
                                                    .setKeyFunction(OpenDialogueAsset::getId)
                                                    .build());
        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogueRouterAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogues/Routers/")
                                                    .setCodec(OpenDialogueRouterAsset.CODEC)
                                                    .setKeyFunction(OpenDialogueRouterAsset::getId)
                                                    .build());
        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogueSpeakerAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogues/Speakers/")
                                                    .setCodec(OpenDialogueSpeakerAsset.CODEC)
                                                    .setKeyFunction(OpenDialogueSpeakerAsset::getId)
                                                    .build());

        onLoaded(OpenDialogueAsset.class, this::validateRouters);
        onLoaded(OpenDialogueRouterAsset.class, this::validateRouters);

        getEventRegistry().registerGlobal(DialogueEvent.Opened.class, historyRecorder::onOpened);
        getEventRegistry().registerGlobal(DialogueEvent.Answered.class, historyRecorder::onAnswered);

        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> dialogueService.forget(event.getPlayerRef()));
        getEventRegistry().register(AssetEditorFetchAutoCompleteDataEvent.class, DialogueRegistry.DISPLAYS_DATA_SET, event -> suggest(event, registry.getDisplayIds()));
        getEventRegistry().register(AssetEditorFetchAutoCompleteDataEvent.class, DialogueRegistry.DIALOGUES_DATA_SET, event -> suggest(event, OpenDialogueAsset.getAssetStore().getAssetMap().getAssetMap().keySet()));

        getCommandRegistry().registerCommand(new DialogueCommand(dialogueService, registry, displayRules));
        getCommandRegistry().registerCommand(new AnswerCommand("oda", dialogueService));
    }

    /**
     * The event is keyed by asset class, which its generic signature cannot express to the registry.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void onLoaded(@Nonnull Class<?> assetClass, @Nonnull Runnable action) {
        getEventRegistry().register((Class) LoadedAssetsEvent.class, assetClass, event -> action.run());
    }

    /**
     * Either store may load or reload first, so every router is checked again against both.
     */
    private void validateRouters() {
        Map<String, String> errors = resolver.validate(OpenDialogueRouterAsset.getAssetStore().getAssetMap().getAssetMap().keySet());
        errors.forEach((routerId, error) -> getLogger().atSevere().log("Router '%s' leads nowhere: %s", routerId, error));
        resolver.setInvalidRouters(errors.keySet());
    }

    private static void suggest(@Nonnull AssetEditorFetchAutoCompleteDataEvent event, @Nonnull Collection<String> ids) {
        String query = event.getQuery().toLowerCase(Locale.ROOT);
        event.setResults(ids.stream().filter(id -> id.toLowerCase(Locale.ROOT).contains(query)).sorted().toArray(String[]::new));
    }

    /**
     * @return where actions, conditions and displays are registered.
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
