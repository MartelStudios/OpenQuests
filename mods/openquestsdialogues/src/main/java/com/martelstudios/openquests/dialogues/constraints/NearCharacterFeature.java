package com.martelstudios.openquests.dialogues.constraints;

import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.extension.journal.QuestPageService;

/**
 * Quests that only move near an NPC carrying a character.
 */
public final class NearCharacterFeature {
    public static final String TYPE_ID = "NearCharacter";

    private NearCharacterFeature() {}

    /**
     * Registers the constraint under its type id, and how the journal describes it.
     */
    public static void register() {
        QuestConstraint.CODEC.register(TYPE_ID, NearCharacterConstraint.class, NearCharacterConstraint.CODEC);
        QuestPageService.register(new NearCharacterRenderer());
    }
}
