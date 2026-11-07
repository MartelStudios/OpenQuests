package com.martelstudios.openquests.extension.quests.castspell;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;

import javax.annotation.Nonnull;

/**
 * Cast spells, the runes slotted in the ability bench, a number of times.
 */
public final class CastSpellFeature {
    public static final String TYPE_ID = "CastSpell";

    private CastSpellFeature() {}

    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(TYPE_ID, CastSpellQuestAsset.class, CastSpellQuestAsset.CODEC, CastSpellQuestProgression.class, CastSpellQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new CastSpellEventSystem(services.getQuestProgressionService()));
    }
}
