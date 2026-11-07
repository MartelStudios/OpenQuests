package com.martelstudios.openquests.extension.map;

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
import com.martelstudios.openquests.extension.quests.composite.CompositeSteps;
import com.martelstudios.openquests.extension.track.QuestTrackService;
import com.martelstudios.openquests.extension.track.events.QuestTrackedEvent;
import com.martelstudios.openquests.extension.track.events.QuestUntrackedEvent;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Running quest locations on the map and compass.
 */
public final class MapFeature {

    /**
     * What the feature set up, for the plugin to expose and to undo on shutdown.
     *
     * @param watcher the packet watcher following players' maps, to deregister.
     */
    public record Registered(@Nonnull QuestTargets targets, @Nonnull PacketFilter watcher) {}

    private MapFeature() {}

    @Nonnull
    public static Registered register(@Nonnull JavaPlugin plugin, @Nonnull OpenQuestsServices services, @Nonnull QuestTrackService tracks) {
        CompositeSteps steps = new CompositeSteps(services.getQuestProgressionService()::getQuest, OpenQuestAsset::getAsset);
        QuestTargets targets = new QuestTargets(services.getQuestProgressionService()::getQuest, steps, tracks::isTracked);
        QuestTargetMarkerProvider provider = new QuestTargetMarkerProvider(targets, services.getQuestPlayerStateService()::getQuestStore);

        plugin.getEventRegistry().registerGlobal(AddWorldEvent.class, event -> event.getWorld().getWorldMapManager().addMarkerProvider(QuestTargetMarkerProvider.ID, provider));
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> provider.forget(event.getPlayerRef().getUuid()));
        onPlayerQuestsChanged(plugin, provider::invalidate);
        PacketFilter watcher = PacketAdapters.registerInbound((PlayerPacketWatcher) (player, packet) -> {
            if (packet instanceof UpdateWorldMapVisible visible) provider.setMapOpen(player.getUuid(), visible.visible);
        });
        return new Registered(targets, watcher);
    }

    /**
     * Calls back with each player whose running quests or tracking changed, so whatever shows
     * their targets computes them again. On the thread that fired the event.
     */
    public static void onPlayerQuestsChanged(@Nonnull JavaPlugin plugin, @Nonnull Consumer<UUID> changed) {
        plugin.getEventRegistry().registerGlobal(QuestAddedToPlayerStoreEvent.class, event -> changed.accept(event.getPlayerId()));
        plugin.getEventRegistry().registerGlobal(QuestCompletedEvent.class, event -> event.getQuest().getPlayers().forEach(changed));
        plugin.getEventRegistry().registerGlobal(QuestPlayerAbandonedEvent.class, event -> changed.accept(event.getPlayerId()));
        plugin.getEventRegistry().registerGlobal(QuestPlayerRemovedEvent.class, event -> changed.accept(event.getPlayerId()));
        plugin.getEventRegistry().registerGlobal(QuestTrackedEvent.class, event -> changed.accept(event.getPlayerId()));
        plugin.getEventRegistry().registerGlobal(QuestUntrackedEvent.class, event -> changed.accept(event.getPlayerId()));
    }
}
