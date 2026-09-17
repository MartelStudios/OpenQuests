package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.extension.listener.QuestListenerService;

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

    private static ComponentType<EntityStore, MovementQuestListener> listenerType;

    private MovementFeature() {}

    /**
     * @return the component naming a player's movement quests, which holds no meaning outside a
     * registered feature.
     */
    public static ComponentType<EntityStore, MovementQuestListener> getListenerType() {
        return listenerType;
    }

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService service = QuestProgressionService.get();

        service.registerQuestType(WALK_TYPE_ID, WalkQuestAsset.class, WalkQuestAsset.CODEC, WalkQuestProgression.class, WalkQuestProgression.CODEC);
        service.registerQuestType(RUN_TYPE_ID, RunQuestAsset.class, RunQuestAsset.CODEC, RunQuestProgression.class, RunQuestProgression.CODEC);
        service.registerQuestType(SPRINT_TYPE_ID, SprintQuestAsset.class, SprintQuestAsset.CODEC, SprintQuestProgression.class, SprintQuestProgression.CODEC);
        service.registerQuestType(JUMP_TYPE_ID, JumpQuestAsset.class, JumpQuestAsset.CODEC, JumpQuestProgression.class, JumpQuestProgression.CODEC);

        // Registered without a codec, so it stays out of the player's file, and on the base type,
        // so the four of them share one component the way they share one system
        listenerType = plugin.getEntityStoreRegistry().registerComponent(MovementQuestListener.class, MovementQuestListener::new);
        QuestListenerService.register(MovementQuestProgression.class, listenerType);

        plugin.getEntityStoreRegistry().registerSystem(new MovementTickingSystem());
    }
}
