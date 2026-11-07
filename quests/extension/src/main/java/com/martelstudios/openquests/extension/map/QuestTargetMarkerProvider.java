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
import java.util.Set;
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

    /** Vanilla image, the one the spec settled on until a dedicated one is drawn. */
    public static final String IMAGE = "Coordinate.png";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * A player's markers as computed for one world.
     */
    private record Built(@Nonnull String world, @Nonnull List<MapMarker> all, @Nonnull List<MapMarker> compass) {}

    private final QuestTargets targets;
    private final Function<UUID, QuestStoreComponent> stores;
    private final Map<UUID, Built> built = new ConcurrentHashMap<>();
    private final Set<UUID> stale = ConcurrentHashMap.newKeySet();
    private final Set<UUID> mapOpen = ConcurrentHashMap.newKeySet();

    /**
     * @param stores a hosted player's quest store, safe from any thread, {@code null} for another.
     */
    public QuestTargetMarkerProvider(@Nonnull QuestTargets targets, @Nonnull Function<UUID, QuestStoreComponent> stores) {
        this.targets = targets;
        this.stores = stores;
    }

    /**
     * Has the player's markers computed again on the next call. Any thread.
     */
    public void invalidate(@Nonnull UUID playerId) {
        stale.add(playerId);
    }

    /**
     * Follows the player's map, which the client reports opening and closing. Any thread.
     */
    public void setMapOpen(@Nonnull UUID playerId, boolean open) {
        if (!open) {
            mapOpen.remove(playerId);
            return;
        }
        mapOpen.add(playerId);
        invalidate(playerId);
    }

    /**
     * Drops what is kept of a player who left.
     */
    public void forget(@Nonnull UUID playerId) {
        built.remove(playerId);
        stale.remove(playerId);
        mapOpen.remove(playerId);
    }

    /**
     * Quest stores are safe from any thread, so the list is computed right here. A closed map
     * shows nothing but the compass, which shows every marker it is given.
     */
    @SuppressWarnings("removal")
    @Override
    public void update(@Nonnull World world, @Nonnull Player player, @Nonnull MarkersCollector collector) {
        UUID playerId = player.getPlayerRef().getUuid();
        Built current = built.get(playerId);
        if (current == null || !current.world.equals(world.getName()) || stale.remove(playerId)) {
            current = build(playerId, world.getName());
            built.put(playerId, current);
        }
        for (MapMarker marker : mapOpen.contains(playerId) ? current.all : current.compass) collector.addIgnoreViewDistance(marker);
    }

    /**
     * A failure leaves an empty list, not retried ten times a second until something changes.
     */
    @Nonnull
    private Built build(@Nonnull UUID playerId, @Nonnull String world) {
        List<MapMarker> all = new ArrayList<>();
        List<MapMarker> compass = new ArrayList<>();
        try {
            QuestStoreComponent store = stores.apply(playerId);
            if (store == null) return new Built(world, List.of(), List.of());
            for (QuestTargets.Running running : targets.runningFor(playerId, store.getQuestIds())) {
                if (!(running.target() instanceof QuestTarget.Location location) || !targets.shownIn(running.quest(), world)) continue;
                MapMarkerBuilder builder = new MapMarkerBuilder("OpenQuests:" + running.quest().getId(), IMAGE, new Transform(location.position())).withName(running.quest().getTitle());
                if (running.tracked()) builder.withCompassImage(IMAGE);
                MapMarker marker = builder.build();
                all.add(marker);
                if (running.tracked()) compass.add(marker);
            }
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Could not compute the quest markers of player %s.", playerId);
            return new Built(world, List.of(), List.of());
        }
        return new Built(world, List.copyOf(all), List.copyOf(compass));
    }
}
