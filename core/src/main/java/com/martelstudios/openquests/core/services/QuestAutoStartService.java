package com.martelstudios.openquests.core.services;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.QuestAsset;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Hands out the quests their asset marks {@code StartOnConnection}, once per player. Only the ids
 * already handed out are kept, so the catalogue is walked once per session and a quest the player
 * never touched costs nothing between sessions.
 */
public class QuestAutoStartService {

    public QuestAutoStartService(JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::handlePlayerConnectEvent);
    }

    public static QuestAutoStartService get() {
        return OpenQuestsCorePlugin.get().getQuestAutoStartService();
    }

    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        var questStore = playerConnectEvent.getHolder().ensureAndGetComponent(QuestStoreComponent.getComponentType());
        UUID playerId = playerConnectEvent.getPlayerRef().getUuid();

        for (QuestAsset asset : QuestAsset.getAssetMap().getAssetMap().values()) {
            if (!asset.isStartOnConnection()) continue;
            if (!questStore.getStartedOnConnection().add(asset.getId())) continue;

            QuestProgressionService.get().registerQuest(asset).addPlayer(playerId);
        }
    }
}
