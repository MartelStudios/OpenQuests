package com.martelstudios.opendialogues.conditions.impl.history;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.schema.metadata.ui.UIEditor;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Holds once the player opened a dialogue at least {@code Min} times, inline dialogues included.
 */
public class SeenCondition extends DialogueCondition {

    public static final String TYPE = "Seen";

    public static final BuilderCodec<SeenCondition> CODEC = BuilderCodec.builder(SeenCondition.class, SeenCondition::new, DialogueCondition.BASE_CODEC)
                                                                        .append(new KeyedCodec<>("Dialogue", Codec.STRING, true), (condition, dialogue) -> condition.dialogue = dialogue, condition -> condition.dialogue)
                                                                        .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                                                                        .add()
                                                                        .append(new KeyedCodec<>("Min", Codec.INTEGER), (condition, min) -> condition.min = min, condition -> condition.min)
                                                                        .add()
                                                                        .build();

    protected String dialogue;
    protected int min = 1;

    protected SeenCondition() {}

    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        return context.history().opened(dialogue) >= min;
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return dialogues.apply(dialogue) == null ? "No dialogue '" + dialogue + "'" : null;
    }
}
