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
 * Holds once the player picked an answer at least {@code Min} times. The answer is named by its
 * {@code Id}, or by its position in the file from 1 when it has none.
 */
public class AnsweredCondition extends DialogueCondition {

    public static final String TYPE = "OpenDialoguesAnswered";

    public static final BuilderCodec<AnsweredCondition> CODEC = BuilderCodec.builder(AnsweredCondition.class, AnsweredCondition::new, DialogueCondition.BASE_CODEC)
                                                                            .append(new KeyedCodec<>("Dialogue", Codec.STRING, true), (condition, dialogue) -> condition.dialogue = dialogue, condition -> condition.dialogue)
                                                                            .metadata(new UIEditor(new UIEditor.TextField(DialogueRegistry.DIALOGUES_DATA_SET)))
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Answer", Codec.STRING, true), (condition, answer) -> condition.answer = answer, condition -> condition.answer)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Min", Codec.INTEGER), (condition, min) -> condition.min = min, condition -> condition.min)
                                                                            .add()
                                                                            .build();

    protected String dialogue;
    protected String answer;
    protected int min = 1;

    protected AnsweredCondition() {}

    @Override
    public boolean test(@Nonnull ConditionContext context) {
        return context.history().answered(dialogue, answer) >= min;
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        OpenDialogueAsset asset = dialogues.apply(dialogue);
        if (asset == null) return "No dialogue '" + dialogue + "'";
        return asset.findAnswer(answer) == null ? "Dialogue '" + dialogue + "' has no answer '" + answer + "'" : null;
    }
}
