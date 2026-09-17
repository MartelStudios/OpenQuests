package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Cover a distance at one of the three paces, or jump a number of times. Registered together
 * because one ticking system samples the player once and answers all of them; split apart they
 * would each read the same components again every tick.
 *
 * <p>The paces are the player's own: {@code Run} is what they travel at by default, {@code Walk}
 * the slower one they hold a key for, {@code Sprint} the faster. Naming them after the movement
 * states rather than after speeds is what keeps them telling each other apart.
 */
public final class MovementFeature {
    public static final String WALK_TYPE_ID = "Walk";
    public static final String RUN_TYPE_ID = "Run";
    public static final String SPRINT_TYPE_ID = "Sprint";
    public static final String JUMP_TYPE_ID = "Jump";

    private MovementFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService service = QuestProgressionService.get();

        service.registerQuestType(WALK_TYPE_ID, WalkQuestAsset.class, WalkQuestAsset.CODEC, WalkQuestProgression.class, WalkQuestProgression.CODEC);
        service.registerQuestType(RUN_TYPE_ID, RunQuestAsset.class, RunQuestAsset.CODEC, RunQuestProgression.class, RunQuestProgression.CODEC);
        service.registerQuestType(SPRINT_TYPE_ID, SprintQuestAsset.class, SprintQuestAsset.CODEC, SprintQuestProgression.class, SprintQuestProgression.CODEC);
        service.registerQuestType(JUMP_TYPE_ID, JumpQuestAsset.class, JumpQuestAsset.CODEC, JumpQuestProgression.class, JumpQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new MovementTickingSystem());
    }
}
