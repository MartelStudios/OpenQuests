package com.martelstudios.opendialogues.conditions.impl.logic;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Combines {@code Conditions} with {@code Operator}: {@code And} holds for an empty list, {@code Or}
 * never does. With {@code Not}, the pair gives "not all" and "none".
 */
public class OpenDialoguesCompositeCondition extends DialogueCondition {

    public static final String TYPE = "Composite";

    public static final BuilderCodec<OpenDialoguesCompositeCondition> CODEC = BuilderCodec.builder(OpenDialoguesCompositeCondition.class, OpenDialoguesCompositeCondition::new, DialogueCondition.BASE_CODEC)
                                                                             .append(new KeyedCodec<>("Operator", new EnumCodec<>(Operator.class)), (condition, operator) -> condition.operator = operator, condition -> condition.operator)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("Conditions", new ArrayCodec<>(DialogueCondition.CODEC, DialogueCondition[]::new)), (condition, conditions) -> condition.conditions = conditions, condition -> condition.conditions)
                                                                             .add()
                                                                             .build();

    protected Operator operator = Operator.AND;
    protected DialogueCondition[] conditions = new DialogueCondition[0];

    protected OpenDialoguesCompositeCondition() {}

    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        boolean any = operator == Operator.OR;
        for (DialogueCondition condition : conditions) {
            if (condition.test(context) == any) return any;
        }
        return !any;
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        for (DialogueCondition condition : conditions) {
            String error = condition.validate(dialogues);
            if (error != null) return error;
        }
        return null;
    }

    /**
     * How the conditions listed combine.
     */
    public enum Operator {
        AND,
        OR
    }
}
