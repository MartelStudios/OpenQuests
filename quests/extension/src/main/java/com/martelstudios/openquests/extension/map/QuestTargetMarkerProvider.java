package com.martelstudios.openquests.extension.map;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MapMarkerBuilder;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.targets.QuestTarget;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Puts the locations a player's running quests lead to on their map, and on their compass while
 * they track the quest. Computes when something changed and replays the result on every call.
 */
public final class QuestTargetMarkerProvider implements WorldMapManager.MarkerProvider {

    /** Id under which every world registers the provider. */
    public static final String ID = "OpenQuestsTargets";

    /** Vanilla image every client already has, so the mod ships none for it. */
    public static final String IMAGE = "Coordinate.png";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * A player's markers as computed for one world, told by UUID so one recreated under the same
     * name is not mistaken for it.
     */
    record Built(@Nonnull UUID world, @Nonnull List<MapMarker> all, @Nonnull List<MapMarker> compass) {}

    private final QuestTargets targets;
    private final Function<UUID, QuestStoreComponent> stores;
    private final Map<UUID, PlayerMarkers> players = new ConcurrentHashMap<>();

    /**
     * @param stores a hosted player's quest store, safe from any thread, {@code null} for another.
     */
    public QuestTargetMarkerProvider(@Nonnull QuestTargets targets, @Nonnull Function<UUID, QuestStoreComponent> stores) {
        this.targets = targets;
        this.stores = stores;
    }

    /**
     * Has the player's markers computed again on the next call. Any thread; a player the provider
     * was never called for is left alone, as an event may name one offline.
     */
    public void invalidate(@Nonnull UUID playerId) {
        players.computeIfPresent(playerId, (id, markers) -> {
            markers.stale = true;
            return markers;
        });
    }

    /**
     * Follows the player's map, which the client reports opening and closing. Any thread.
     */
    public void setMapOpen(@Nonnull UUID playerId, boolean open) {
        if (!open) {
            players.computeIfPresent(playerId, (id, markers) -> {
                markers.mapOpen = false;
                return markers;
            });
            return;
        }
        // The client only sends it while connected, so the entry may be created here
        PlayerMarkers markers = players.computeIfAbsent(playerId, id -> new PlayerMarkers());
        markers.mapOpen = true;
        markers.stale = true;
    }

    /**
     * Drops what is kept of a player who left.
     */
    public void forget(@Nonnull UUID playerId) {
        players.remove(playerId);
    }

    /**
     * @return whether anything is kept of the player.
     */
    boolean knows(@Nonnull UUID playerId) {
        return players.containsKey(playerId);
    }

    /**
     * Quest stores are safe from any thread, so the list is computed right here. A closed map
     * shows nothing but the compass, which shows every marker it is given.
     */
    @SuppressWarnings("removal")
    @Override
    public void update(@Nonnull World world, @Nonnull Player player, @Nonnull MarkersCollector collector) {
        UUID playerId = player.getPlayerRef().getUuid();
        PlayerMarkers markers = players.get(playerId);
        if (markers == null) {
            // A call that comes after the player left must not keep them
            if (stores.apply(playerId) == null) return;
            markers = players.computeIfAbsent(playerId, id -> new PlayerMarkers());
        }
        Built current = markers.built;
        if (current == null || !current.world.equals(world.getWorldConfig().getUuid()) || markers.stale) {
            markers.stale = false;
            current = build(playerId, world);
            markers.built = current;
        }
        for (MapMarker marker : markers.mapOpen ? current.all : current.compass) collector.addIgnoreViewDistance(marker);
    }

    /**
     * A failure leaves an empty list, not retried ten times a second until something changes.
     */
    @Nonnull
    private Built build(@Nonnull UUID playerId, @Nonnull World in) {
        UUID world = in.getWorldConfig().getUuid();
        try {
            QuestStoreComponent store = stores.apply(playerId);
            return store == null ? new Built(world, List.of(), List.of()) : build(world, in.getName(), targets.runningFor(playerId, store.getQuestIds()));
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Could not compute the quest markers of player %s.", playerId);
            return new Built(world, List.of(), List.of());
        }
    }

    /**
     * @return one marker per running quest leading to a location shown in that world, on the
     * compass while the quest is tracked.
     */
    @Nonnull
    Built build(@Nonnull UUID world, @Nonnull String worldName, @Nonnull List<QuestTargets.Running> running) {
        List<MapMarker> all = new ArrayList<>();
        List<MapMarker> compass = new ArrayList<>();
        for (QuestTargets.Running quest : running) {
            if (!(quest.target() instanceof QuestTarget.Location location) || !targets.shownIn(quest.quest(), worldName)) continue;
            MapMarkerBuilder builder = new MapMarkerBuilder("OpenQuests:" + quest.quest().getId(), IMAGE, new Transform(location.position())).withName(quest.quest().getTitle());
            if (quest.tracked()) builder.withCompassImage(IMAGE);
            MapMarker marker = builder.build();
            all.add(marker);
            if (quest.tracked()) compass.add(marker);
        }
        return new Built(world, List.copyOf(all), List.copyOf(compass));
    }
}
