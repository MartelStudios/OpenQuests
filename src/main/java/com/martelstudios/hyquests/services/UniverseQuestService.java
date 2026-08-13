package com.martelstudios.hyquests.services;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.hyquests.HyQuestsPlugin;
import com.martelstudios.hyquests.models.AbstractQuest;
import com.martelstudios.hyquests.stores.QuestsRecord;
import com.martelstudios.hyquests.stores.QuestsStore;

import javax.annotation.Nonnull;
import java.util.UUID;

public class UniverseQuestService {
    private static final String UNIVERSE_QUEST_INDEX_KEY = "universe";
    private static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Quests shared by every player regardless of the world.
     */
    public QuestsRecord universeQuests = new QuestsRecord();
    private final QuestsStore questsStore;

    public UniverseQuestService(QuestsStore questsStore) {
        this.questsStore = questsStore;
    }

    public static UniverseQuestService get() {
        return HyQuestsPlugin.get().universeQuestService;
    }

    /**
     * Adds a quest to the universe's index and to every currently connected player.
     */
    public void addQuestToUniverse(@Nonnull UUID questId) {
        AbstractQuest<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest == null) return;

        universeQuests.register(questId);
        PlayerQuestService.get().addQuestToPlayerStore(questId, Universe.get().getPlayers());
    }

    /**
     * Removes a quest from the universe's index and from every connected player, unless they
     * hold it directly (in the quest's own player list) or through a world they're currently in
     * (in the quest's own world list).
     */
    public void removeQuestFromUniverse(@Nonnull UUID questId) {
        AbstractQuest<?> quest = QuestProgressionService.get().getQuest(questId);
        if (quest == null) return;

        universeQuests.unregister(questId);

        for (PlayerRef player : Universe.get().getPlayers()) {
            // If the quest holds a direct player reference, skip the player
            if (quest.getPlayers().contains(player.getUuid())) continue;

            // If the quest holds a world reference of the player's current world, skip the player
            UUID currentWorldUuid = player.getWorldUuid();
            if (currentWorldUuid != null && quest.getWorlds().contains(currentWorldUuid)) continue;

            // Otherwise, remove the quest from the player's store
            var ref = player.getReference();
            if (ref == null) continue;
            var world = ref.getStore().getExternalData().getWorld();
            world.execute(() -> PlayerQuestService.get().removeQuestFromPlayerStore(questId, ref));
        }
    }

    /**
     * Loads the universe-scope quest index and pulls every quest it lists into the datastore.
     */
    public void loadQuests() {
        QuestsRecord questsRecord = questsStore.load(UNIVERSE_QUEST_INDEX_KEY);
        if (questsRecord == null) return;

        universeQuests = questsRecord;
        for (UUID questId : universeQuests.getAllIds()) {
            QuestProgressionService.get().loadQuest(questId);
        }
    }

    public void saveUniverseQuestIndex() {
        if (questsStore == null) return;
        questsStore.save(UNIVERSE_QUEST_INDEX_KEY);
    }

}
