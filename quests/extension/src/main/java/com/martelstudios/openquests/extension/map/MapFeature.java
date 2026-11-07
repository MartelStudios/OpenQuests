package com.martelstudios.openquests.extension.map;

import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMapVisible;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerAbandonedEvent;
import com.martelstudios.openquests.core.events.QuestPlayerRemovedEvent;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.extension.map.events.PlayerQuestTargetsChangedEvent;
import com.martelstudios.openquests.extension.quests.composite.CompositeSteps;
import com.martelstudios.openquests.extension.track.QuestTrackService;
import com.martelstudios.openquests.extension.track.events.QuestTrackedEvent;
import com.martelstudios.openquests.extension.track.events.QuestUntrackedEvent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Running quest locations on the map and compass.
 */
public final class MapFeature {

    private final QuestTargets targets;
    private final PacketFilter watcher;

    /**
     * Registers the location markers in every world, and fires {@link PlayerQuestTargetsChangedEvent}
     * for whatever else shows quest targets.
     */
    public MapFeature(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services, @Nonnull QuestTrackService tracks) {
        CompositeSteps steps = new CompositeSteps(services.getQuestProgressionService()::getQuest, OpenQuestAsset::getAsset);
        targets = new QuestTargets(services.getQuestProgressionService()::getQuest, steps, tracks::isTracked);
        QuestTargetMarkerProvider provider = new QuestTargetMarkerProvider(targets, services.getQuestPlayerStateService()::getQuestStore);

        plugin.getEventRegistry().registerGlobal(AddWorldEvent.class, event -> event.getWorld().getWorldMapManager().addMarkerProvider(QuestTargetMarkerProvider.ID, provider));
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> provider.forget(event.getPlayerRef().getUuid()));
        plugin.getEventRegistry().registerGlobal(PlayerQuestTargetsChangedEvent.class, event -> provider.invalidate(event.getPlayerId()));
        fireOnChange(plugin, services.events());
        watcher = PacketAdapters.registerInbound((PlayerPacketWatcher) (player, packet) -> {
            if (packet instanceof UpdateWorldMapVisible visible) provider.setMapOpen(player.getUuid(), visible.visible);
        });
    }

    /**
     * @return where each player's running quests lead.
     */
    @Nonnull
    public QuestTargets targets() {
        return targets;
    }

    /**
     * Stops following players' maps, as the packet watcher outlives the plugin otherwise.
     */
    public void shutdown() {
        PacketAdapters.deregisterInbound(watcher);
    }

    private static void fireOnChange(@Nonnull JavaPlugin plugin, @Nonnull IEventBus events) {
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, event -> fire(events, event.getPlayerId()));
        plugin.getEventRegistry().registerGlobal(QuestCompletedEvent.class, event -> event.getQuest().getPlayers().forEach(player -> fire(events, player)));
        plugin.getEventRegistry().registerGlobal(QuestPlayerAbandonedEvent.class, event -> fire(events, event.getPlayerId()));
        plugin.getEventRegistry().registerGlobal(QuestPlayerRemovedEvent.class, event -> fire(events, event.getPlayerId()));
        plugin.getEventRegistry().registerGlobal(QuestTrackedEvent.class, event -> fire(events, event.getPlayerId()));
        plugin.getEventRegistry().registerGlobal(QuestUntrackedEvent.class, event -> fire(events, event.getPlayerId()));
    }

    private static void fire(@Nonnull IEventBus events, @Nonnull UUID playerId) {
        events.dispatchFor(PlayerQuestTargetsChangedEvent.class, null).dispatch(new PlayerQuestTargetsChangedEvent(playerId));
    }
}
