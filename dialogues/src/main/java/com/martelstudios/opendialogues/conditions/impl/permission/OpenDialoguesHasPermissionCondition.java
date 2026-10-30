package com.martelstudios.opendialogues.conditions.impl.permission;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;

import javax.annotation.Nonnull;

/**
 * Holds while the player has a permission, such as the one a command asks before running as them.
 */
public class OpenDialoguesHasPermissionCondition extends DialogueCondition {

    public static final String TYPE = "HasPermission";

    public static final BuilderCodec<OpenDialoguesHasPermissionCondition> CODEC = BuilderCodec.builder(OpenDialoguesHasPermissionCondition.class, OpenDialoguesHasPermissionCondition::new, DialogueCondition.BASE_CODEC)
                                                                                 .append(new KeyedCodec<>("Permission", Codec.STRING, true), (condition, permission) -> condition.permission = permission, condition -> condition.permission)
                                                                                 .addValidator(Validators.nonNull())
                                                                                 .addValidator(Validators.nonEmptyString())
                                                                                 .add()
                                                                                 .build();

    protected String permission;

    protected OpenDialoguesHasPermissionCondition() {}

    /**
     * Fails without a player, as when only the history is judged.
     */
    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        return context.player() != null && context.player().hasPermission(permission);
    }
}
