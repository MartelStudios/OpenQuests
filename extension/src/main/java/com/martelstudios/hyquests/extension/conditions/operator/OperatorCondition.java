package com.martelstudios.hyquests.extension.conditions.operator;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.martelstudios.hyquests.core.assignment.conditions.QuestAssignmentCondition;
import com.martelstudios.hyquests.core.assignment.conditions.QuestAssignmentConditionResolver;

import javax.annotation.Nonnull;

/**
 * Combines nested conditions with a boolean operator, allowing arbitrarily deep trees.
 */
public class OperatorCondition extends QuestAssignmentCondition<OperatorCondition> {

    public static final BuilderCodec<OperatorCondition> CODEC = BuilderCodec.builder(OperatorCondition.class, OperatorCondition::new, QuestAssignmentCondition.BASE_CODEC)
                                                                            .append(new KeyedCodec<>("Not", Codec.BOOLEAN), (condition, not) -> condition.not = not, condition -> Boolean.valueOf(condition.not))
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Operator", new EnumCodec<>(Operator.class)), (condition, operator) -> condition.operator = operator, condition -> condition.operator)
                                                                            .add()
                                                                            .append(new KeyedCodec<>("Conditions", new ArrayCodec<>(QuestAssignmentCondition.CODEC, QuestAssignmentCondition<?>[]::new)), (condition, children) -> condition.conditions = children, condition -> condition.conditions)
                                                                            .add()
                                                                            .build();

    protected Operator operator = Operator.AND;
    protected boolean not;
    protected QuestAssignmentCondition<?>[] conditions = new QuestAssignmentCondition[0];

    private OperatorCondition() {}

    /**
     * Latches only if every child does: one momentary child makes the whole tree momentary.
     */
    @Override
    public boolean useCache() {
        for (var condition : conditions) {
            if (!condition.useCache()) return false;
        }
        return true;
    }

    @Override
    public QuestAssignmentConditionResolver<OperatorCondition> getResolver() {
        return OperatorConditionFeature.getResolver();
    }

    @Nonnull
    public QuestAssignmentCondition<?>[] getConditions() {
        return conditions;
    }

    public enum Operator {
        AND, OR, XOR
    }
}
