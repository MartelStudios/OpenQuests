package com.martelstudios.opendialogues;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.event.RemovedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.martelstudios.opendialogues.actions.impl.close.CloseDialogueAction;
import com.martelstudios.opendialogues.actions.impl.command.CommandDialogueAction;
import com.martelstudios.opendialogues.actions.impl.open.OpenDialogueAction;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.commands.AnswerCommand;
import com.martelstudios.opendialogues.commands.DialogueCommand;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.dialogues.OpenDialogueSpeakerAsset;
import com.martelstudios.opendialogues.nodeeditor.NodeDefinition;
import com.martelstudios.opendialogues.nodeeditor.WorkspaceBuilder;
import com.martelstudios.opendialogues.nodeeditor.WorkspaceExporter;
import com.martelstudios.opendialogues.presenters.impl.chat.ChatPresenter;
import com.martelstudios.opendialogues.presenters.impl.page.PagePresenter;
import com.martelstudios.opendialogues.services.DialogueService;

import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.dialogues.DialogueResponse;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Dialogues and their speakers as assets: written in the Asset Editor or linked in the Node Editor,
 * opened by id and drawn by whichever presenter they name.
 */
public class OpenDialoguesPlugin extends JavaPlugin {

    public static final String NODES = "/NodeEditor/OpenDialogues/";

    private static OpenDialoguesPlugin instance;

    private final DialogueRegistry registry = new DialogueRegistry();

    private DialogueService dialogueService;

    public OpenDialoguesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static OpenDialoguesPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        registry.registerNode(node(WorkspaceBuilder.ROOT_NODE, "Dialogue", "Dialogue.json"));
        registry.registerNode(node("OpenDialogues.Page", "Dialogue", "Page.json"));
        registry.registerNode(node("OpenDialogues.Response", "Dialogue", "Response.json"));
        registry.registerAction(CloseDialogueAction.TYPE, CloseDialogueAction.class, CloseDialogueAction.CODEC, node("OpenDialogues.CloseAction", "Actions", "CloseAction.json"));
        registry.registerAction(OpenDialogueAction.TYPE, OpenDialogueAction.class, OpenDialogueAction.CODEC, node("OpenDialogues.OpenAction", "Actions", "OpenAction.json"));
        registry.registerAction(CommandDialogueAction.TYPE, CommandDialogueAction.class, CommandDialogueAction.CODEC, node("OpenDialogues.CommandAction", "Actions", "CommandAction.json"));
        registry.registerNode(node("OpenDialogues.Speaker", "Dialogue", "Speaker.json"));
        registry.registerNode(node(WorkspaceBuilder.SPEAKER_ROOT_NODE, "Speaker", "SpeakerAsset.json"));

        // shortcut: the chat is the default presenter for every dialogue naming none, make it a server setting when servers ask.
        dialogueService = new DialogueService(registry, ChatPresenter.ID);
        registry.registerPresenter(ChatPresenter.ID, new ChatPresenter());
        registry.registerPresenter(PagePresenter.ID, new PagePresenter(dialogueService));

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

        // The workspace lists the dialogue and speaker ids, so it follows every change to them
        getEventRegistry().register(LoadedAssetsEvent.class, OpenDialogueAsset.class, this::onDialoguesLoaded);
        getEventRegistry().register(RemovedAssetsEvent.class, OpenDialogueAsset.class, this::onDialoguesRemoved);
        getEventRegistry().register(LoadedAssetsEvent.class, OpenDialogueSpeakerAsset.class, this::onSpeakersLoaded);
        getEventRegistry().register(RemovedAssetsEvent.class, OpenDialogueSpeakerAsset.class, this::onSpeakersRemoved);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> dialogueService.forget(event.getPlayerRef()));

        getCommandRegistry().registerCommand(new DialogueCommand(dialogueService));
        getCommandRegistry().registerCommand(new AnswerCommand("oda", dialogueService));
    }

    @Override
    protected void start() {
        exportWorkspace();
    }

    /**
     * @return where actions, Node Editor nodes and presenters are registered.
     */
    @Nonnull
    public DialogueRegistry getRegistry() {
        return registry;
    }

    @Nonnull
    public DialogueService getDialogueService() {
        return dialogueService;
    }

    private void onDialoguesLoaded(@Nonnull LoadedAssetsEvent<String, OpenDialogueAsset, DefaultAssetMap<String, OpenDialogueAsset>> event) {
        event.getLoadedAssets().values().forEach(OpenDialogueAsset::nameInlineDialogues);
        exportWorkspace();
    }

    private void onDialoguesRemoved(@Nonnull RemovedAssetsEvent<String, OpenDialogueAsset, DefaultAssetMap<String, OpenDialogueAsset>> event) {
        exportWorkspace();
    }

    private void onSpeakersLoaded(@Nonnull LoadedAssetsEvent<String, OpenDialogueSpeakerAsset, DefaultAssetMap<String, OpenDialogueSpeakerAsset>> event) {
        exportWorkspace();
    }

    private void onSpeakersRemoved(@Nonnull RemovedAssetsEvent<String, OpenDialogueSpeakerAsset, DefaultAssetMap<String, OpenDialogueSpeakerAsset>> event) {
        exportWorkspace();
    }

    private void exportWorkspace() {
        try {
            Set<String> dialogueIds = new HashSet<>();
            Set<String> speakerIds = new HashSet<>(OpenDialogueSpeakerAsset.getAssetStore().getAssetMap().getAssetMap().keySet());
            for (OpenDialogueAsset dialogue : OpenDialogueAsset.getAssetStore().getAssetMap().getAssetMap().values()) {
                dialogueIds.add(dialogue.getId());
                collectReferences(dialogue, dialogueIds, speakerIds);
            }
            var files = WorkspaceBuilder.build(registry.getNodes(), registry.getActionNodes(), Map.of(WorkspaceBuilder.DIALOGUE_IDS, dialogueIds, WorkspaceBuilder.SPEAKER_IDS, speakerIds));
            var result = WorkspaceExporter.export(WorkspaceExporter.findWorkspacesFolders(AssetModule.get().getBaseAssetPack().getPackLocation()), files);
            result.written().forEach(folder -> getLogger().atInfo().log("Node Editor workspace written to %s", folder));
            result.failed().forEach((folder, error) -> getLogger().atWarning().withCause(error).log("Could not write the Node Editor workspace into %s", folder));
        } catch (IOException e) {
            getLogger().atWarning().withCause(e).log("Could not build the Node Editor workspace");
        }
    }

    /**
     * Adds every dialogue and speaker id the dialogue names, loaded or not, since the Node Editor
     * crashes on an id missing from its list: see {@link DialogueAction#getReferencedDialogues}.
     */
    private static void collectReferences(@Nonnull OpenDialogueAsset dialogue, @Nonnull Set<String> dialogueIds, @Nonnull Set<String> speakerIds) {
        if (dialogue.getSpeaker() != null && dialogue.getSpeaker().getReference() != null) {
            speakerIds.add(dialogue.getSpeaker().getReference());
        }
        for (DialogueResponse response : dialogue.getResponses()) {
            for (DialogueAction action : response.getActions()) {
                dialogueIds.addAll(action.getReferencedDialogues());
                action.getInlineDialogues().forEach(inline -> collectReferences(inline, dialogueIds, speakerIds));
            }
        }
    }

    @Nonnull
    private static NodeDefinition node(@Nonnull String id, @Nonnull String category, @Nonnull String file) {
        return new NodeDefinition(id, category, OpenDialoguesPlugin.class.getResource(NODES + file));
    }
}
