package com.martelstudios.opendialogues.conditions;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.conditions.impl.history.AnsweredCondition;
import com.martelstudios.opendialogues.conditions.impl.history.SeenCondition;
import com.martelstudios.opendialogues.conditions.impl.logic.AllCondition;
import com.martelstudios.opendialogues.conditions.impl.logic.AnyCondition;
import com.martelstudios.opendialogues.conditions.impl.logic.NotCondition;
import com.martelstudios.opendialogues.history.DialogueHistory;

import javax.annotation.Nonnull;

/**
 * Registers the conditions tests read, once per run, as the plugin does at setup.
 */
public final class ConditionFixtures {

    static {
        DialogueRegistry registry = new DialogueRegistry();
        registry.registerCondition(AllCondition.TYPE, AllCondition.class, AllCondition.CODEC);
        registry.registerCondition(AnyCondition.TYPE, AnyCondition.class, AnyCondition.CODEC);
        registry.registerCondition(NotCondition.TYPE, NotCondition.class, NotCondition.CODEC);
        registry.registerCondition(SeenCondition.TYPE, SeenCondition.class, SeenCondition.CODEC);
        registry.registerCondition(AnsweredCondition.TYPE, AnsweredCondition.class, AnsweredCondition.CODEC);
    }

    private ConditionFixtures() {}

    @Nonnull
    public static DialogueCondition condition(@Nonnull String json) {
        DialogueCondition condition = CodecJson.decode(DialogueCondition.CODEC, json);
        if (condition == null) throw new IllegalArgumentException("Unreadable condition " + json);
        return condition;
    }

    @Nonnull
    public static ConditionContext context(@Nonnull DialogueHistory history) {
        return new ConditionContext(null, null, null, history);
    }
}
