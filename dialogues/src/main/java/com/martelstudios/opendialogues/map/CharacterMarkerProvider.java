package com.martelstudios.opendialogues.map;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MapMarkerBuilder;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.MarkersCollector;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositions;
import com.martelstudios.opendialogues.characters.runtime.CharacterPositionsAccess;
import com.martelstudios.opendialogues.history.DialogueHistory;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Puts the NPCs carrying a character on each player's map, and on their compass when another mod
 * says so. Decides which characters show on the player's world thread when something the player did
 * changes; follows NPC moves by joining that decision with the positions, without asking again.
 */
public final class CharacterMarkerProvider implements WorldMapManager.MarkerProvider {

    /** Id under which every world registers the provider. */
    public static final String ID = "OpenDialoguesCharacters";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * How one character's NPCs show to a player.
     *
     * @param epithet      the name of an NPC that drew none, {@code null} for none.
     * @param compassRange within how many blocks an NPC also shows on the compass, {@code 0} for none.
     */
    record Look(@Nonnull CharacterMarkerRule.Shown shown, @Nullable String epithet, double compassRange) {}

    /**
     * A marker the compass shows only while the player stands within {@code range} blocks of it.
     */
    record Near(@Nonnull MapMarker marker, double range) {}

    /**
     * Which characters a player sees in one world, told by UUID so a world recreated under the same
     * name is not mistaken for it.
     *
     * @param positions the world's positions, {@code null} when the computation failed: kept until
     *                  the next invalidation rather than retried at every call.
     * @param decided   every character judged, shown or not; another one appearing calls for a recompute.
     */
    record Seen(@Nonnull UUID world, @Nullable CharacterPositions positions, @Nonnull Set<String> decided, @Nonnull Map<String, Look> shown) {}

    /**
     * A player's markers, from one decision and one version of the positions.
     *
     * @param undecided whether some NPC carries a character the decision did not judge.
     */
    record Built(@Nonnull Seen from, int version, @Nonnull List<MapMarker> all, @Nonnull List<MapMarker> compass, @Nonnull List<Near> near, boolean undecided) {

        /**
         * A closed map shows nothing but the compass, which shows every marker it is given.
         *
         * @param player where the player stands, which brings the {@link Near} markers in range.
         */
        void replay(boolean mapOpen, @Nonnull Vector3d player, @Nonnull MarkersCollector collector) {
            if (mapOpen) {
                for (MapMarker marker : all) collector.addIgnoreViewDistance(marker);
                return;
            }
            for (MapMarker marker : compass) collector.addIgnoreViewDistance(marker);
            for (Near near : near) {
                Position at = near.marker().transform.position;
                if (player.distanceSquared(at.x, at.y, at.z) <= near.range() * near.range()) collector.addIgnoreViewDistance(near.marker());
            }
        }
    }

    private final CharacterPositionsAccess positions;
    private final Function<PlayerRef, DialogueHistory> histories;
    private final Function<String, OpenDialogueCharacterAsset> assets;
    private final List<CharacterStatusProvider> statusProviders = new CopyOnWriteArrayList<>();
    private final Map<UUID, PlayerMarkers> players = new ConcurrentHashMap<>();

    /**
     * @param positions only read here: the scan and releases write them.
     * @param histories a player's dialogue history, read on their world thread, {@code null} elsewhere.
     * @param assets    finds a loaded character by id, {@code null} for one no pack declares.
     */
    public CharacterMarkerProvider(@Nonnull CharacterPositionsAccess positions, @Nonnull Function<PlayerRef, DialogueHistory> histories, @Nonnull Function<String, OpenDialogueCharacterAsset> assets) {
        this.positions = positions;
        this.histories = histories;
        this.assets = assets;
    }

    /**
     * Lets another mod reveal characters and set their icon, a quest leading to them for instance.
     */
    public void addStatusProvider(@Nonnull CharacterStatusProvider provider) {
        statusProviders.add(provider);
    }

    /**
     * Has the player's markers computed again, as what decides them changed. Any thread; a player
     * the provider was never called for is left alone, as an event may name one offline.
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
     * @return the player's entry, {@code null} for one never called for or forgotten.
     */
    @Nullable
    PlayerMarkers entry(@Nonnull UUID playerId) {
        return players.get(playerId);
    }

    /**
     * Runs on the world map thread: never reads a component, only schedules a recompute and joins
     * its result with the positions, which are safe to read from any thread. The server hands a
     * {@code Player} alone, whose identity getters are all marked for removal.
     */
    @SuppressWarnings("removal")
    @Override
    public void update(@Nonnull World world, @Nonnull Player player, @Nonnull MarkersCollector collector) {
        PlayerRef playerRef = player.getPlayerRef();
        UUID playerId = playerRef.getUuid();
        PlayerMarkers markers = players.get(playerId);
        if (markers == null) {
            // A call that comes after the player left must not keep them
            if (!playerRef.isValid()) return;
            markers = players.computeIfAbsent(playerId, id -> new PlayerMarkers());
        }
        PlayerMarkers entry = markers;
        Built built = follow(entry, world.getWorldConfig().getUuid(), () -> world.execute(() -> settle(playerId, entry, world.getWorldConfig().getUuid(), () -> decide(world, playerId, entry))));
        // The transform is a plain field the world thread writes: a torn read only misjudges one call
        if (built != null) built.replay(entry.mapOpen, playerRef.getTransform().getPosition(), collector);
    }

    /**
     * Asks for a recompute when the player changed world or something they did changed, at most one
     * waiting at a time, then joins what was decided with the current positions.
     *
     * @param recompute queues {@link #settle} on the world thread.
     * @return the markers to show, {@code null} while nothing is decided for this world or it failed.
     */
    @Nullable
    Built follow(@Nonnull PlayerMarkers markers, @Nonnull UUID worldId, @Nonnull Runnable recompute) {
        Seen seen = markers.seen;
        boolean here = seen != null && seen.world.equals(worldId);
        if ((!here || markers.stale) && markers.pending.compareAndSet(false, true)) recompute.run();
        if (!here || seen.positions == null) return null;

        // Read before the view, so a change in between only makes the next call join again
        int version = seen.positions.version();
        Built built = markers.built;
        if (built == null || built.from != seen || built.version != version) {
            built = join(seen, version, seen.positions.view());
            markers.built = built;
            if (built.undecided) markers.stale = true;
        }
        return built;
    }

    /**
     * Keeps what {@code decide} returns, unless the player was forgotten meanwhile. A failure keeps
     * an empty decision until the next invalidation rather than retrying at every call.
     *
     * @param decide {@code null} when the player is gone or elsewhere, which decides nothing.
     */
    void settle(@Nonnull UUID playerId, @Nonnull PlayerMarkers markers, @Nonnull UUID worldId, @Nonnull Supplier<Seen> decide) {
        boolean wasStale = markers.stale;
        try {
            markers.stale = false;
            Seen seen = decide.get();
            if (seen == null) {
                if (wasStale) markers.stale = true;
                return;
            }
            if (players.get(playerId) == markers) markers.seen = seen;
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Could not compute the character markers of player %s.", playerId);
            if (players.get(playerId) == markers) markers.seen = new Seen(worldId, null, Set.of(), Map.of());
        } finally {
            markers.pending.set(false);
        }
    }

    /**
     * On the world thread, where the history and the positions' reset check may be read.
     */
    @Nullable
    private Seen decide(@Nonnull World world, @Nonnull UUID playerId, @Nonnull PlayerMarkers markers) {
        UUID worldId = world.getWorldConfig().getUuid();
        PlayerRef playerRef = Universe.get().getPlayer(playerId);
        if (playerRef == null) {
            players.remove(playerId, markers);
            return null;
        }
        if (!worldId.equals(playerRef.getWorldUuid())) return null;
        CharacterPositions positions = this.positions.positions(world.getEntityStore().getStore());
        Set<String> decided = Set.copyOf(positions.view().keySet());
        DialogueHistory history = histories.apply(playerRef);
        return new Seen(worldId, positions, decided, looks(decided, id -> history != null && history.count(id, null, null) > 0, statuses(playerId)));
    }


    /**
     * A failing provider, often another mod's, must not take every marker away.
     */
    @Nonnull
    private Map<String, List<CharacterMapStatus>> statuses(@Nonnull UUID playerId) {
        Map<String, List<CharacterMapStatus>> statuses = new HashMap<>();
        for (CharacterStatusProvider provider : statusProviders) {
            try {
                provider.statusesFor(playerId).forEach((id, status) -> statuses.computeIfAbsent(id, key -> new ArrayList<>()).add(status));
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("Character status provider %s failed.", provider.getClass().getName());
            }
        }
        return statuses;
    }

    /**
     * @param met whether the player talked to a character.
     * @return how each shown character looks; one left out stays off the map.
     */
    @Nonnull
    Map<String, Look> looks(@Nonnull Collection<String> characterIds, @Nonnull Predicate<String> met, @Nonnull Map<String, List<CharacterMapStatus>> statuses) {
        Map<String, Look> looks = new HashMap<>();
        for (String characterId : characterIds) {
            OpenDialogueCharacterAsset character = assets.apply(characterId);
            if (character == null) continue;
            CharacterMarkerRule.Shown shown = CharacterMarkerRule.resolve(character.getMapVisibility(), character.getMapIcon(), met.test(characterId), CharacterMarkerRule.merge(statuses.getOrDefault(characterId, List.of())));
            if (shown != null) looks.put(characterId, new Look(shown, character.getEpithet(), character.getCompassRange()));
        }
        return Map.copyOf(looks);
    }

    /**
     * @return one marker per NPC of each shown character.
     */
    @Nonnull
    static Built join(@Nonnull Seen seen, int version, @Nonnull Map<String, Collection<CharacterPositions.Spot>> spots) {
        List<MapMarker> all = new ArrayList<>();
        List<MapMarker> compass = new ArrayList<>();
        List<Near> near = new ArrayList<>();
        boolean undecided = false;
        for (Map.Entry<String, Collection<CharacterPositions.Spot>> entry : spots.entrySet()) {
            Look look = seen.shown.get(entry.getKey());
            if (look == null) {
                undecided |= !seen.decided.contains(entry.getKey());
                continue;
            }
            for (CharacterPositions.Spot spot : entry.getValue()) {
                MapMarker marker = marker(spot, look, spot.name() != null ? spot.name() : look.epithet());
                all.add(marker);
                if (look.shown().onCompass()) compass.add(marker);
                else if (look.compassRange() > 0) near.add(new Near(marker, look.compassRange()));
            }
        }
        return new Built(seen, version, List.copyOf(all), List.copyOf(compass), List.copyOf(near), undecided);
    }

    @Nonnull
    private static MapMarker marker(@Nonnull CharacterPositions.Spot spot, @Nonnull Look look, @Nullable String name) {
        CharacterMarkerRule.Shown shown = look.shown();
        MapMarkerBuilder builder = new MapMarkerBuilder("OpenDialogues:" + spot.npc(), shown.image(), new Transform(spot.position()));
        if (name != null) builder.withName(Message.translation(name));
        if (shown.onCompass() || look.compassRange() > 0) builder.withCompassImage(shown.image());
        return builder.build();
    }
}
