package com.martelstudios.hyquests.extension.conditions.operator;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.assignment.conditions.QuestAssignmentCondition;
import com.martelstudios.hyquests.extension.conditions.queststate.QuestStateConditionResolver;

import javax.annotation.Nonnull;

public final class OperatorConditionFeature {
    public static final String TYPE_ID = "Operator";

    private static final OperatorConditionResolver RESOLVER = new OperatorConditionResolver();

    private OperatorConditionFeature() {}

    public static OperatorConditionResolver getResolver() {
        return RESOLVER;
    }

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestAssignmentCondition.CODEC.register(TYPE_ID, OperatorCondition.class, OperatorCondition.CODEC);
    }
}
