package com.martelstudios.openquests.core.services;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
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

        for (OpenQuestAsset asset : OpenQuestAsset.getAssetMap().getAssetMap().values()) {
            if (!asset.isStartOnConnection()) continue;
            if (questStore.getStartedOnConnection().contains(asset.getId())) continue;

            // Only written down once handed out, so a quest its constraints refused is offered again
            if (QuestProgressionService.get().assignQuest(asset, playerId) == null) continue;

            questStore.getStartedOnConnection().add(asset.getId());
            questStore.markDirty();
        }
    }
}
