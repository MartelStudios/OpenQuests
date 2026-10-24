package com.martelstudios.opendialogs;

import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.martelstudios.opendialogs.actions.CloseDialogAction;
import com.martelstudios.opendialogs.actions.CommandDialogAction;
import com.martelstudios.opendialogs.actions.DialogAction;
import com.martelstudios.opendialogs.actions.OpenDialogAction;
import com.martelstudios.opendialogs.commands.DialogCommand;
import com.martelstudios.opendialogs.dialogs.OpenDialogsDialog;
import com.martelstudios.opendialogs.nodeeditor.WorkspaceExporter;
import com.martelstudios.opendialogs.services.DialogService;

import javax.annotation.Nonnull;

/**
 * Dialogs as assets: written in the Asset Editor or linked in the Node Editor, opened by id.
 */
public class OpenDialogsPlugin extends JavaPlugin {

    private static OpenDialogsPlugin instance;

    private DialogService dialogService;

    public OpenDialogsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static OpenDialogsPlugin get() {
        return instance;
    }

    @Override
    protected void setup() {
        super.setup();

        dialogService = new DialogService();

        DialogAction.CODEC.register(CloseDialogAction.TYPE, CloseDialogAction.class, CloseDialogAction.CODEC);
        DialogAction.CODEC.register(OpenDialogAction.TYPE, OpenDialogAction.class, OpenDialogAction.CODEC);
        DialogAction.CODEC.register(CommandDialogAction.TYPE, CommandDialogAction.class, CommandDialogAction.CODEC);

        getAssetRegistry().register(HytaleAssetStore.builder(OpenDialogsDialog.class, new DefaultAssetMap<>())
                                                    .setPath("OpenDialogs/Dialogs/")
                                                    .setCodec(OpenDialogsDialog.CODEC)
                                                    .setKeyFunction(OpenDialogsDialog::getId)
                                                    .build());

        getCommandRegistry().registerCommand(new DialogCommand(dialogService));
    }

    @Override
    protected void start() {
        var result = WorkspaceExporter.export(WorkspaceExporter.findWorkspacesFolders(AssetModule.get().getBaseAssetPack().getPackLocation()));
        result.written().forEach(folder -> getLogger().atInfo().log("Node Editor workspace written to %s", folder));
        result.failed().forEach((folder, error) -> getLogger().atWarning().withCause(error).log("Could not write the Node Editor workspace into %s", folder));
    }

    public DialogService getDialogService() {
        return dialogService;
    }
}
