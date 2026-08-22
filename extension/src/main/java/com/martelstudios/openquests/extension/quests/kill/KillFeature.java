package com.martelstudios.openquests.extension.quests.kill;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.extension.quests.kill.npc.KillNpcDeathSystem;
import com.martelstudios.openquests.extension.quests.kill.npc.KillNpcQuestAsset;
import com.martelstudios.openquests.extension.quests.kill.npc.KillNpcQuestProgression;
import com.martelstudios.openquests.extension.quests.kill.player.KillPlayerDeathSystem;
import com.martelstudios.openquests.extension.quests.kill.player.KillPlayerQuestAsset;
import com.martelstudios.openquests.extension.quests.kill.player.KillPlayerQuestProgression;

import javax.annotation.Nonnull;

/**
 * Kill quests, driven by the victim's death rather than by the damage that caused it.
 */
public final class KillFeature {
    public static final String KILL_PLAYER_TYPE_ID = "KillPlayer";
    public static final String KILL_NPC_TYPE_ID = "KillNpc";

    private KillFeature() {}

    public static void register(@Nonnull JavaPlugin plugin) {
        QuestProgressionService.get()
                               .registerQuestType(KILL_PLAYER_TYPE_ID, KillPlayerQuestAsset.class, KillPlayerQuestAsset.CODEC, KillPlayerQuestProgression.class, KillPlayerQuestProgression.CODEC);

        QuestProgressionService.get()
                               .registerQuestType(KILL_NPC_TYPE_ID, KillNpcQuestAsset.class, KillNpcQuestAsset.CODEC, KillNpcQuestProgression.class, KillNpcQuestProgression.CODEC);

        plugin.getEntityStoreRegistry().registerSystem(new KillPlayerDeathSystem());
        plugin.getEntityStoreRegistry().registerSystem(new KillNpcDeathSystem());
    }
}
