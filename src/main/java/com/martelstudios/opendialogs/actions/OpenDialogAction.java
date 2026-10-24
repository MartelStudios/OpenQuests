package com.martelstudios.opendialogs.actions;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogs.services.DialogService;

import javax.annotation.Nonnull;

/**
 * Moves on to another dialog, the way {@code /odialogs open} would.
 */
public class OpenDialogAction extends DialogAction {

    public static final String TYPE = "OpenDialogsOpen";

    public static final BuilderCodec<OpenDialogAction> CODEC = BuilderCodec.builder(OpenDialogAction.class, OpenDialogAction::new, DialogAction.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("Dialog", Codec.STRING), (action, dialog) -> action.dialog = dialog, action -> action.dialog)
                                                                           .addValidator(Validators.nonNull())
                                                                           .add()
                                                                           .build();

    private String dialog;

    @Override
    public void execute(@Nonnull DialogService dialogs, @Nonnull PlayerRef player) {
        dialogs.open(player, dialog);
    }
}
