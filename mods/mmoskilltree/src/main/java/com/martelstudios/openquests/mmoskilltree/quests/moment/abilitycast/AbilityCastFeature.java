package com.martelstudios.openquests.mmoskilltree.quests.moment.abilitycast;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.ziggfreed.mmoskilltree.ability.cast.AbilityCastObserverRegistry;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cast an ability a number of times: one ability, any ability of a skill, or any at all.
 */
public final class AbilityCastFeature {
    public static final String TYPE_ID = "MMOSkillTreeAbilityCast";

    /**
     * Cast observers cannot be removed, so a plugin set up again must not add a second one.
     */
    private static final AtomicBoolean OBSERVED = new AtomicBoolean();

    private AbilityCastFeature() {}

    /**
     * Registers the type and the cast observer feeding it.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(TYPE_ID, MMOSkillTreeAbilityCastQuestAsset.class, MMOSkillTreeAbilityCastQuestAsset.CODEC, AbilityCastQuestProgression.class, AbilityCastQuestProgression.CODEC);

        if (OBSERVED.compareAndSet(false, true)) AbilityCastObserverRegistry.getInstance().register(AbilityCastEvents::handleAbilityCast);
    }
}
