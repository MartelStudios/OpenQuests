package com.martelstudios.openquests.extension.constraints.death;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.constraints.QuestConstraint;

import javax.annotation.Nonnull;

/**
 * Quests a death ends.
 */
public final class DeathConstraintsFeature {
    public static final String FAIL_ON_DEATH_TYPE_ID = "FailOnDeath";

    private DeathConstraintsFeature() {}

    /**
     * Registers the constraint under its type id, and the system hearing the deaths.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestConstraint.CODEC.register(FAIL_ON_DEATH_TYPE_ID, FailOnDeathConstraint.class, FailOnDeathConstraint.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new FailOnDeathSystem());
    }
}
