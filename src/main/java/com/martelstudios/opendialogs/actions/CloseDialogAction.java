package com.martelstudios.opendialogs.actions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogs.services.DialogService;

import javax.annotation.Nonnull;

/**
 * Ends the dialog.
 */
public class CloseDialogAction extends DialogAction {

    public static final String TYPE = "OpenDialogsClose";

    public static final BuilderCodec<CloseDialogAction> CODEC = BuilderCodec.builder(CloseDialogAction.class, CloseDialogAction::new, DialogAction.BASE_CODEC).build();

    @Override
    public void execute(@Nonnull DialogService dialogs, @Nonnull PlayerRef player) {
        dialogs.close(player);
    }
}
