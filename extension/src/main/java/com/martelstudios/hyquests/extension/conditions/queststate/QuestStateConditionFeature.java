package com.martelstudios.hyquests.extension.conditions.queststate;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.hyquests.core.assignment.conditions.QuestAssignmentCondition;
import com.martelstudios.hyquests.core.events.QuestCompletedEvent;

import javax.annotation.Nonnull;

public final class QuestStateConditionFeature {
    public static final String TYPE_ID = "QuestState";

    private static final QuestStateConditionResolver RESOLVER = new QuestStateConditionResolver();

    private QuestStateConditionFeature() {}

    public static QuestStateConditionResolver getResolver() {
        return RESOLVER;
    }

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestAssignmentCondition.CODEC.register(TYPE_ID, QuestStateCondition.class, QuestStateCondition.CODEC);

        plugin.getEventRegistry().registerGlobal(QuestCompletedEvent.class, RESOLVER::handleQuestCompletedEvent);
    }
}
