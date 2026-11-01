package com.martelstudios.opendialogues;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.asseteditor.event.AssetEditorFetchAutoCompleteDataEvent;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.event.events.BootEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.martelstudios.opendialogues.actions.impl.close.OpenDialoguesCloseAction;
import com.martelstudios.opendialogues.actions.impl.command.OpenDialoguesCommandAction;
import com.martelstudios.opendialogues.actions.impl.inventory.OpenDialoguesConsumeInventoryAction;
import com.martelstudios.opendialogues.actions.impl.open.OpenDialoguesOpenAction;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.OpenDialogueDistributionAsset;
import com.martelstudios.opendialogues.characters.OpenDialogueNameListAsset;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.CharacterValidator;
import com.martelstudios.opendialogues.characters.runtime.CharacterDeathSystem;
import com.martelstudios.opendialogues.characters.runtime.CharacterHintSystem;
import com.martelstudios.opendialogues.characters.runtime.CharacterRemovalSystem;
import com.martelstudios.opendialogues.characters.runtime.CharacterScanSystem;
import com.martelstudios.opendialogues.characters.runtime.CharacterService;
import com.martelstudios.opendialogues.characters.runtime.NpcEligibility;
import com.martelstudios.opendialogues.characters.runtime.CharacterSettings;
import com.martelstudios.opendialogues.characters.runtime.CharacterTalkSystem;
import com.martelstudios.opendialogues.characters.runtime.OpenDialoguesNPCIdentity;
import com.martelstudios.opendialogues.characters.runtime.UniqueCharacters;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesBiomeFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesGroupFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesRoleFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesWorldFilter;
import com.martelstudios.opendialogues.characters.filters.impl.OpenDialoguesZoneFilter;
import com.martelstudios.opendialogues.characters.traits.impl.OpenDialoguesDialogueTrait;
import com.martelstudios.opendialogues.characters.traits.impl.OpenDialoguesNameTrait;
import com.martelstudios.opendialogues.commands.AnswerCommand;
import com.martelstudios.opendialogues.commands.DialogueCommand;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialogueSpeakerAsset;
import com.martelstudios.opendialogues.displays.DisplayRules;
import com.martelstudios.opendialogues.events.DialogueEvent;
import com.martelstudios.opendialogues.routers.OpenDialogueRouterAsset;
import com.martelstudios.opendialogues.routers.impl.sequence.OpenDialoguesSequenceSelector;
import com.martelstudios.opendialogues.routers.impl.weighted.OpenDialoguesWeightedSelector;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.history.DialogueHistoryRecorder;
import com.martelstudios.opendialogues.displays.impl.chat.ChatDisplay;
import com.martelstudios.opendialogues.displays.impl.page.CinematicDialoguePage;
import com.martelstudios.opendialogues.displays.impl.page.DialogueCustomPage;
import com.martelstudios.opendialogues.displays.impl.page.PageDisplay;
import com.martelstudios.opendialogues.services.DialogueService;
import com.martelstudios.opendialogues.triggers.BuilderActionOpenDialogue;
import com.martelstudios.opendialogues.triggers.OpenDialoguesOpenInteraction;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;


import com.martelstudios.opendialogues.conditions.impl.history.OpenDialoguesAnsweredCondition;
import com.martelstudios.opendialogues.conditions.impl.history.OpenDialoguesOpenedCondition;
import com.martelstudios.opendialogues.conditions.impl.items.OpenDialoguesHoldingCondition;
import com.martelstudios.opendialogues.conditions.impl.items.OpenDialoguesInventoryCondition;
import com.martelstudios.opendialogues.conditions.impl.permission.OpenDialoguesHasPermissionCondition;
import com.martelstudios.opendialogues.conditions.impl.logic.OpenDialoguesCompositeCondition;

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

    private final Config<CharacterSettings> characterSettings;

    private DialogueService dialogueService;

    private CharacterService characterService;

    private ComponentType<EntityStore, DialogueHistory> historyType;

    private ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType;

    private ResourceType<EntityStore, UniqueCharacters> uniqueCharactersType;

    private DialogueHistoryRecorder historyRecorder;

    private DialogueResolver resolver;

    /**
     * Until then packs are half loaded: a router's inline dialogues load before the pack's
     * dialogue folder, so a check would report targets that are only on their way.
     */
    private volatile boolean booted;

    public OpenDialoguesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
        displayRules = withConfig("Displays", DisplayRules.CODEC);
        characterSettings = withConfig("Characters", CharacterSettings.CODEC);
    }

    public static OpenDialoguesPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        registry.registerAction(OpenDialoguesCloseAction.TYPE, OpenDialoguesCloseAction.class, OpenDialoguesCloseAction.CODEC);
        registry.registerAction(OpenDialoguesOpenAction.TYPE, OpenDialoguesOpenAction.class, OpenDialoguesOpenAction.CODEC);
        getCodecRegistry(Interaction.CODEC).register(OpenDialoguesOpenInteraction.TYPE, OpenDialoguesOpenInteraction.class, OpenDialoguesOpenInteraction.CODEC);
        NPCPlugin.get().registerCoreComponentType(BuilderActionOpenDialogue.TYPE, BuilderActionOpenDialogue::new);
        registry.registerAction(OpenDialoguesCommandAction.TYPE, OpenDialoguesCommandAction.class, OpenDialoguesCommandAction.CODEC);
        registry.registerAction(OpenDialoguesConsumeInventoryAction.TYPE, OpenDialoguesConsumeInventoryAction.class, OpenDialoguesConsumeInventoryAction.CODEC);

        registry.registerCondition(OpenDialoguesCompositeCondition.TYPE, OpenDialoguesCompositeCondition.class, OpenDialoguesCompositeCondition.CODEC);
        registry.registerCondition(OpenDialoguesOpenedCondition.TYPE, OpenDialoguesOpenedCondition.class, OpenDialoguesOpenedCondition.CODEC);
        registry.registerCondition(OpenDialoguesAnsweredCondition.TYPE, OpenDialoguesAnsweredCondition.class, OpenDialoguesAnsweredCondition.CODEC);
        registry.registerCondition(OpenDialoguesHoldingCondition.TYPE, OpenDialoguesHoldingCondition.class, OpenDialoguesHoldingCondition.CODEC);
        registry.registerCondition(OpenDialoguesInventoryCondition.TYPE, OpenDialoguesInventoryCondition.class, OpenDialoguesInventoryCondition.CODEC);
        registry.registerCondition(OpenDialoguesHasPermissionCondition.TYPE, OpenDialoguesHasPermissionCondition.class, OpenDialoguesHasPermissionCondition.CODEC);

        registry.registerFilter(OpenDialoguesRoleFilter.TYPE, OpenDialoguesRoleFilter.class, OpenDialoguesRoleFilter.CODEC);
        registry.registerFilter(OpenDialoguesGroupFilter.TYPE, OpenDialoguesGroupFilter.class, OpenDialoguesGroupFilter.CODEC);
        registry.registerFilter(OpenDialoguesWorldFilter.TYPE, OpenDialoguesWorldFilter.class, OpenDialoguesWorldFilter.CODEC);
        registry.registerFilter(OpenDialoguesZoneFilter.TYPE, OpenDialoguesZoneFilter.class, OpenDialoguesZoneFilter.CODEC);
        registry.registerFilter(OpenDialoguesBiomeFilter.TYPE, OpenDialoguesBiomeFilter.class, OpenDialoguesBiomeFilter.CODEC);

        registry.registerTrait(OpenDialoguesDialogueTrait.TYPE, OpenDialoguesDialogueTrait.class, OpenDialoguesDialogueTrait.CODEC);
        registry.registerTrait(OpenDialoguesNameTrait.TYPE, OpenDialoguesNameTrait.class, OpenDialoguesNameTrait.CODEC);

        registry.registerSelector(OpenDialoguesSequenceSelector.TYPE, OpenDialoguesSequenceSelector.class, OpenDialoguesSequenceSelector.CODEC);
        registry.registerSelector(OpenDialoguesWeightedSelector.TYPE, OpenDialoguesWeightedSelector.class, OpenDialoguesWeightedSelector.CODEC);

        historyType = getEntityStoreRegistry().registerComponent(DialogueHistory.class, "OpenDialoguesHistory", DialogueHistory.CODEC);
        historyRecorder = new DialogueHistoryRecorder(historyType);
        identityType = getEntityStoreRegistry().registerComponent(OpenDialoguesNPCIdentity.class, "OpenDialoguesNPCIdentity", OpenDialoguesNPCIdentity.CODEC);
        uniqueCharactersType = getEntityStoreRegistry().registerResource(UniqueCharacters.class, "OpenDialoguesUniqueCharacters", UniqueCharacters.CODEC);
        characterService = new CharacterService(CharacterAssets.live(), NpcEligibility::test, HytaleServer.get().getEventBus(), new SplittableRandom(), (message, error) -> getLogger().atWarning().withCause(error).log("%s", message));
        getEntityStoreRegistry().registerSystem(new CharacterScanSystem(characterSettings.get(), characterService, identityType, uniqueCharactersType));
        getEntityStoreRegistry().registerSystem(new CharacterTalkSystem(characterService, identityType, uniqueCharactersType));
        getEntityStoreRegistry().registerSystem(new CharacterHintSystem(characterService, identityType));
        getEntityStoreRegistry().registerSystem(new CharacterDeathSystem(characterService, identityType, uniqueCharactersType));
        getEntityStoreRegistry().registerSystem(new CharacterRemovalSystem(characterService, identityType, uniqueCharactersType));
        resolver = new DialogueResolver(OpenDialogueAsset::getDialogue, OpenDialogueRouterAsset::getRouter, new SplittableRandom());
        dialogueService = new DialogueService(registry, displayRules::get, ChatDisplay.ID, resolver, player -> ConditionContext.of(player, historyRecorder.history(player)), HytaleServer.get().getEventBus());
        registry.registerDisplay(ChatDisplay.ID, new ChatDisplay());
        registry.registerDisplay(PageDisplay.ID, new PageDisplay(dialogueService, DialogueCustomPage::new));
        registry.registerDisplay(PageDisplay.CINEMATIC_ID, new PageDisplay(dialogueService, CinematicDialoguePage::new));

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
        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogueNameListAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogues/Names/")
                                                    .setCodec(OpenDialogueNameListAsset.CODEC)
                                                    .setKeyFunction(OpenDialogueNameListAsset::getId)
                                                    .build());
        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogueCharacterAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogues/Characters/")
                                                    .setCodec(OpenDialogueCharacterAsset.CODEC)
                                                    .setKeyFunction(OpenDialogueCharacterAsset::getId)
                                                    .build());
        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogueDistributionAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogues/Distributions/")
                                                    .setCodec(OpenDialogueDistributionAsset.CODEC)
                                                    .setKeyFunction(OpenDialogueDistributionAsset::getId)
                                                    .build());

        onLoaded(OpenDialogueAsset.class, this::validateRouters);
        onLoaded(OpenDialogueAsset.class, this::validateAnswers);
        onLoaded(OpenDialogueRouterAsset.class, this::validateRouters);
        onLoaded(OpenDialogueCharacterAsset.class, this::validateCharacters);
        onLoaded(OpenDialogueCharacterAsset.class, () -> characterService.forgetFailures());
        onLoaded(OpenDialogueDistributionAsset.class, this::validateCharacters);
        getEventRegistry().register(BootEvent.class, event -> {
            booted = true;
            validateRouters();
            validateAnswers();
            validateCharacters();
        });

        getEventRegistry().registerGlobal(DialogueEvent.Opened.class, historyRecorder::onOpened);
        getEventRegistry().registerGlobal(DialogueEvent.Answered.class, historyRecorder::onAnswered);

        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> dialogueService.forget(event.getPlayerRef()));
        getEventRegistry().register(AssetEditorFetchAutoCompleteDataEvent.class, DialogueRegistry.DISPLAYS_DATA_SET, event -> suggest(event, registry.getDisplayIds()));
        getEventRegistry().register(AssetEditorFetchAutoCompleteDataEvent.class, DialogueRegistry.DIALOGUES_DATA_SET, event -> suggest(event, OpenDialogueAsset.getAssetStore().getAssetMap().getAssetMap().keySet()));

        getCommandRegistry().registerCommand(new DialogueCommand(dialogueService, registry, displayRules, historyRecorder));
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
        if (booted) errors.forEach((routerId, error) -> getLogger().atSevere().log("Router '%s' leads nowhere: %s", routerId, error));
        resolver.setInvalidRouters(errors.keySet());
    }

    /**
     * Only logged: a broken answer condition is still judged, as an unknown id never holds.
     */
    private void validateAnswers() {
        if (!booted) return;
        for (OpenDialogueAsset dialogue : OpenDialogueAsset.getAssetStore().getAssetMap().getAssetMap().values()) {
            String error = dialogue.findConditionError(OpenDialogueAsset::getDialogue);
            if (error != null) getLogger().atSevere().log("Dialogue '%s' has a broken answer condition: %s", dialogue.getId(), error);
        }
    }

    /**
     * Only logged: a character leading nowhere still names its NPC, pressing F just opens nothing.
     */
    private void validateCharacters() {
        if (!booted) return;
        for (String problem : CharacterValidator.validate(CharacterAssets.live())) getLogger().atSevere().log("%s", problem);
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

    /**
     * @return the persisted history of what each player opened and answered, for mods reading it
     * through their own component access, online or offline.
     */
    @Nonnull
    public ComponentType<EntityStore, DialogueHistory> getHistoryType() {
        return historyType;
    }

    /**
     * @return who each NPC became, saved on the NPC, for bridges finding a given NPC's character.
     */
    @Nonnull
    public ComponentType<EntityStore, OpenDialoguesNPCIdentity> getIdentityType() {
        return identityType;
    }

    /**
     * @return the unique characters each world gave away, saved with the world.
     */
    @Nonnull
    public ResourceType<EntityStore, UniqueCharacters> getUniqueCharactersType() {
        return uniqueCharactersType;
    }

    /**
     * @return gives, applies and takes back NPC characters, for bridges and commands, on the world thread.
     */
    @Nonnull
    public CharacterService getCharacterService() {
        return characterService;
    }
}
