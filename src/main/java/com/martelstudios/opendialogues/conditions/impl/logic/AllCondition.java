package com.martelstudios.opendialogues.conditions.impl.logic;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Holds when every condition listed holds, an empty list included.
 */
public class AllCondition extends DialogueCondition {

    public static final String TYPE = "All";

    public static final BuilderCodec<AllCondition> CODEC = BuilderCodec.builder(AllCondition.class, AllCondition::new, DialogueCondition.BASE_CODEC)
                                                                       .append(new KeyedCodec<>("Conditions", new ArrayCodec<>(DialogueCondition.CODEC, DialogueCondition[]::new)), (condition, conditions) -> condition.conditions = conditions, condition -> condition.conditions)
                                                                       .add()
                                                                       .build();

    protected DialogueCondition[] conditions = new DialogueCondition[0];

    protected AllCondition() {}

    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        for (DialogueCondition condition : conditions) {
            if (!condition.test(context)) return false;
        }
        return true;
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return LogicConditions.validateAll(conditions, dialogues);
    }
}
