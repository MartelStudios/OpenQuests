package com.martelstudios.openquests.extension.conditions.operator;

import com.martelstudios.openquests.core.assignment.assets.QuestAssignmentAsset;
import com.martelstudios.openquests.core.assignment.conditions.QuestAssignmentConditionResolver;

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
}
