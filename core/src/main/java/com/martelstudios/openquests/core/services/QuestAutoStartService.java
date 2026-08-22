package com.martelstudios.openquests.core.services;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.OpenQuestCorePlugin;
import com.martelstudios.openquests.core.assets.QuestAsset;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Hands out the quests their asset marks {@code StartOnConnection}, once per player. A quest is
 * handed out at its baseline: until it progresses it is persisted nowhere and is simply handed
 * out again next session, so a catalogue nobody touched costs nothing.
 */
public class QuestAutoStartService {

    public QuestAutoStartService(JavaPlugin plugin) {
        plugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::handlePlayerConnectEvent);
    }

    public static QuestAutoStartService get() {
        return OpenQuestCorePlugin.get().getQuestAutoStartService();
    }

    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        var questStore = playerConnectEvent.getHolder().ensureAndGetComponent(QuestStoreComponent.getComponentType());
        UUID playerId = playerConnectEvent.getPlayerRef().getUuid();

        for (QuestAsset asset : QuestAsset.getAssetMap().getAssetMap().values()) {
            if (!asset.isStartOnConnection()) continue;
            if (!questStore.getStartedOnConnection().add(asset.getId())) continue;

            AbstractQuestProgression<?> quest = QuestProgressionService.get().registerQuest(asset);
            quest.addPlayer(playerId);
            quest.markPristine();
        }
    }
}
