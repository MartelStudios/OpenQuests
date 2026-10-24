package com.martelstudios.opendialogues;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.martelstudios.opendialogues.actions.CloseDialogueAction;
import com.martelstudios.opendialogues.actions.CommandDialogueAction;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.actions.OpenDialogueAction;
import com.martelstudios.opendialogues.commands.DialogueCommand;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import com.martelstudios.opendialogues.nodeeditor.WorkspaceExporter;
import com.martelstudios.opendialogues.services.DialogueService;

import javax.annotation.Nonnull;

/**
 * Dialogues as assets: written in the Asset Editor or linked in the Node Editor, opened by id.
 */
public class OpenDialoguesPlugin extends JavaPlugin {

    private static OpenDialoguesPlugin instance;

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

        dialogueService = new DialogueService();

        DialogueAction.CODEC.register(CloseDialogueAction.TYPE, CloseDialogueAction.class, CloseDialogueAction.CODEC);
        DialogueAction.CODEC.register(OpenDialogueAction.TYPE, OpenDialogueAction.class, OpenDialogueAction.CODEC);
        DialogueAction.CODEC.register(CommandDialogueAction.TYPE, CommandDialogueAction.class, CommandDialogueAction.CODEC);

        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogueAsset.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogues/Dialogues/")
                                                    .setCodec(OpenDialogueAsset.CODEC)
                                                    .setKeyFunction(OpenDialogueAsset::getId)
                                                    .build());

        getCommandRegistry().registerCommand(new DialogueCommand(dialogueService));
    }

    @Override
    protected void start() {
        var result = WorkspaceExporter.export(WorkspaceExporter.findWorkspacesFolders(AssetModule.get().getBaseAssetPack().getPackLocation()));
        result.written().forEach(folder -> getLogger().atInfo().log("Node Editor workspace written to %s", folder));
        result.failed().forEach((folder, error) -> getLogger().atWarning().withCause(error).log("Could not write the Node Editor workspace into %s", folder));
    }

    public DialogueService getDialogueService() {
        return dialogueService;
    }
}
