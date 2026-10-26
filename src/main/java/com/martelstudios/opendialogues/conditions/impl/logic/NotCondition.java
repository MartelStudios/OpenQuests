package com.martelstudios.opendialogues.conditions.impl.logic;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Holds when the condition it wraps does not.
 */
public class NotCondition extends DialogueCondition {

    public static final String TYPE = "Not";

    public static final BuilderCodec<NotCondition> CODEC = BuilderCodec.builder(NotCondition.class, NotCondition::new, DialogueCondition.BASE_CODEC)
                                                                       .append(new KeyedCodec<>("Condition", DialogueCondition.CODEC, true), (condition, inner) -> condition.condition = inner, condition -> condition.condition)
                                                                       .add()
                                                                       .build();

    protected DialogueCondition condition;

    protected NotCondition() {}

    @Override
    public boolean test(@Nonnull ConditionContext context) {
        return !condition.test(context);
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return condition.validate(dialogues);
    }
}
