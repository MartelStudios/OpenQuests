package com.martelstudios.opendialogues.conditions.impl.logic;

import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

final class LogicConditions {

    private LogicConditions() {}

    @Nullable
    static String validateAll(@Nonnull DialogueCondition[] conditions, @Nonnull Function<String, OpenDialogueAsset> dialogues) {
        for (DialogueCondition condition : conditions) {
            String error = condition.validate(dialogues);
            if (error != null) return error;
        }
        return null;
    }
}
