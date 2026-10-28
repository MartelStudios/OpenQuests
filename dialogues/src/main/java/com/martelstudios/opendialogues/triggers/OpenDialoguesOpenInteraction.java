package com.martelstudios.opendialogues.triggers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.api.DialogueRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An interaction items call, on secondary use for instance, to open a dialogue for the player using them.
 */
public class OpenDialoguesOpenInteraction extends SimpleInstantInteraction {

    public static final String TYPE = "OpenDialoguesOpen";

    public static final BuilderCodec<OpenDialoguesOpenInteraction> CODEC = BuilderCodec.builder(OpenDialoguesOpenInteraction.class, OpenDialoguesOpenInteraction::new, SimpleInstantInteraction.CODEC)
                                                                                       .append(new KeyedCodec<>("Dialogue", Codec.STRING, true), (interaction, dialogue) -> interaction.dialogue = dialogue, interaction -> interaction.dialogue)
                                                                                       .addValidator(Validators.nonEmptyString())
                                                                                       .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                                                                                       .add()
                                                                                       .build();

    @Nullable
    private String dialogue;

    /**
     * @return the id of the dialogue or router opened.
     */
    @Nullable
    public String getDialogue() {
        return dialogue;
    }

    @Override
    protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        PlayerRef player = context.getCommandBuffer().getComponent(context.getEntity(), PlayerRef.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }
        DialogueTriggers.openFor(player, dialogue, "Interaction '" + getId() + "'");
    }
}
