package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

public final class MovementFeature {
    public static final String WALK_TYPE_ID = "Walk";
    public static final String RUN_TYPE_ID = "Run";
    public static final String SPRINT_TYPE_ID = "Sprint";
    public static final String JUMP_TYPE_ID = "Jump";

    private MovementFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        QuestProgressionService service = services.getQuestProgressionService();
        service.registerQuestType(WALK_TYPE_ID, WalkQuestAsset.class, WalkQuestAsset.CODEC, WalkQuestProgression.class, WalkQuestProgression.CODEC);
        service.registerQuestType(RUN_TYPE_ID, RunQuestAsset.class, RunQuestAsset.CODEC, RunQuestProgression.class, RunQuestProgression.CODEC);
        service.registerQuestType(SPRINT_TYPE_ID, SprintQuestAsset.class, SprintQuestAsset.CODEC, SprintQuestProgression.class, SprintQuestProgression.CODEC);
        service.registerQuestType(JUMP_TYPE_ID, JumpQuestAsset.class, JumpQuestAsset.CODEC, JumpQuestProgression.class, JumpQuestProgression.CODEC);
    }
}
