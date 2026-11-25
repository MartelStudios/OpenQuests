package com.martelstudios.openquests.extension.quests.loot;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.extension.quests.loot.npc.LootNpcDeathSystem;
import com.martelstudios.openquests.extension.quests.loot.npc.LootNpcQuestAsset;
import com.martelstudios.openquests.extension.quests.loot.npc.LootNpcQuestProgression;

import javax.annotation.Nonnull;

/**
 * Loot quests: items that exist only for the quest, dropped by what the player kills.
 */
public final class LootFeature {
    public static final String LOOT_NPC_TYPE_ID = "LootNpc";

    private LootFeature() {}

    /**
     * Registers the {@code LootNpc} type and the death system dropping its items.
     */
    public static void register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services) {
        services.getQuestProgressionService().registerQuestType(LOOT_NPC_TYPE_ID, LootNpcQuestAsset.class, LootNpcQuestAsset.CODEC, LootNpcQuestProgression.class, LootNpcQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new LootNpcDeathSystem(services.getQuestProgressionService()));
    }
}
