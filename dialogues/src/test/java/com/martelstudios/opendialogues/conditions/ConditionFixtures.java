package com.martelstudios.opendialogues.conditions;

import com.martelstudios.opendialogues.conditions.impl.world.OpenDialoguesNearBlockCondition;
import com.martelstudios.opendialogues.conditions.impl.world.OpenDialoguesNearNpcCondition;
import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.api.DialogueRegistry;
import com.martelstudios.opendialogues.conditions.impl.history.OpenDialoguesAnsweredCondition;
import com.martelstudios.opendialogues.conditions.impl.history.OpenDialoguesCooldownCondition;
import com.martelstudios.opendialogues.conditions.impl.history.OpenDialoguesOpenedAfterCondition;
import com.martelstudios.opendialogues.conditions.impl.permission.OpenDialoguesHasPermissionCondition;
import com.martelstudios.opendialogues.conditions.impl.history.OpenDialoguesOpenedCondition;
import com.martelstudios.opendialogues.conditions.impl.logic.OpenDialoguesCompositeCondition;
import com.martelstudios.opendialogues.history.DialogueHistory;
import com.martelstudios.opendialogues.sessions.DialogueSource;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Registers the conditions tests read, once per run, as the plugin does at setup.
 */
public final class ConditionFixtures {

    static {
        DialogueRegistry registry = new DialogueRegistry();
        registry.registerCondition(OpenDialoguesCompositeCondition.TYPE, OpenDialoguesCompositeCondition.class, OpenDialoguesCompositeCondition.CODEC);
        registry.registerCondition(OpenDialoguesOpenedCondition.TYPE, OpenDialoguesOpenedCondition.class, OpenDialoguesOpenedCondition.CODEC);
        registry.registerCondition(OpenDialoguesAnsweredCondition.TYPE, OpenDialoguesAnsweredCondition.class, OpenDialoguesAnsweredCondition.CODEC);
        registry.registerCondition(OpenDialoguesCooldownCondition.TYPE, OpenDialoguesCooldownCondition.class, OpenDialoguesCooldownCondition.CODEC);
        registry.registerCondition(OpenDialoguesOpenedAfterCondition.TYPE, OpenDialoguesOpenedAfterCondition.class, OpenDialoguesOpenedAfterCondition.CODEC);
        registry.registerCondition(OpenDialoguesHasPermissionCondition.TYPE, OpenDialoguesHasPermissionCondition.class, OpenDialoguesHasPermissionCondition.CODEC);
        registry.registerCondition(OpenDialoguesNearNpcCondition.TYPE, OpenDialoguesNearNpcCondition.class, OpenDialoguesNearNpcCondition.CODEC);
        registry.registerCondition(OpenDialoguesNearBlockCondition.TYPE, OpenDialoguesNearBlockCondition.class, OpenDialoguesNearBlockCondition.CODEC);
    }

    private ConditionFixtures() {}

    /**
     * Registers the conditions before decoding JSON that names them outside {@link #condition}.
     */
    public static void registerConditions() {
        // Calling any member runs the static block.
    }

    @Nonnull
    public static DialogueCondition condition(@Nonnull String json) {
        DialogueCondition condition = CodecJson.decode(DialogueCondition.CODEC, json);
        if (condition == null) throw new IllegalArgumentException("Unreadable condition " + json);
        return condition;
    }

    @Nonnull
    public static ConditionContext context(@Nonnull DialogueHistory history) {
        return new ConditionContext(null, null, null, history, null);
    }

    @Nonnull
    public static ConditionContext context(@Nonnull DialogueHistory history, @Nullable DialogueSource source) {
        return new ConditionContext(null, null, null, history, source);
    }
}
