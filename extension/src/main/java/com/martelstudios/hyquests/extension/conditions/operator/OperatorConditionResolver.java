package com.martelstudios.hyquests.extension.conditions.operator;

import com.martelstudios.hyquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.hyquests.core.assignment.conditions.QuestAssignmentConditionResolver;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import java.util.UUID;

public class OperatorConditionResolver implements QuestAssignmentConditionResolver<OperatorCondition> {
    @Override
    public void register(@Nonnull QuestAssignmentAsset asset, @Nonnull OperatorCondition condition) {
        for (var subCondition : condition.getConditions()) {
            subCondition.register(asset);
        }
    }

    @Override
    public boolean evaluate(OperatorCondition operatorCondition, @Nonnull UUID playerId) {
        var operatorValue = switch (operatorCondition.operator) {
            case AND -> checkAnd(operatorCondition, playerId);
            case OR -> checkOr(operatorCondition, playerId);
            case XOR -> checkXor(operatorCondition, playerId);
        };

        return operatorCondition.not != operatorValue;
    }

    private boolean checkAnd(OperatorCondition operatorCondition, @Nonnull UUID playerId) {
        for (var condition : operatorCondition.getConditions()) {
            if (!condition.evaluate(playerId)) return false;
        }
        return true;
    }

    private boolean checkOr(OperatorCondition operatorCondition, @Nonnull UUID playerId) {
        for (var condition : operatorCondition.getConditions()) {
            if (condition.evaluate(playerId)) return true;
        }
        return false;
    }

    /**
     * Odd-parity fold, so it degrades to "one or the other" for two conditions. Cannot
     * short-circuit: every child has to be evaluated.
     */
    private boolean checkXor(OperatorCondition operatorCondition, @Nonnull UUID playerId) {
        boolean result = false;
        for (var condition : operatorCondition.getConditions()) {
            result ^= condition.evaluate(playerId);
        }
        return result;
    }
}
