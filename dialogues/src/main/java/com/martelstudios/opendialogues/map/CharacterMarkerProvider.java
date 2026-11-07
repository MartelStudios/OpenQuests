package com.martelstudios.opendialogues.map;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
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
     * @param epithet the name of an NPC that drew none, {@code null} for none.
     */
    record Look(@Nonnull CharacterMarkerRule.Shown shown, @Nullable String epithet) {}

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
    record Built(@Nonnull Seen from, int version, @Nonnull List<MapMarker> all, @Nonnull List<MapMarker> compass, boolean undecided) {

        /**
         * A closed map shows nothing but the compass, which shows every marker it is given.
         */
        void replay(boolean mapOpen, @Nonnull MarkersCollector collector) {
            for (MapMarker marker : mapOpen ? all : compass) collector.addIgnoreViewDistance(marker);
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
     * @return whether anything is kept of the player.
     */
    boolean knows(@Nonnull UUID playerId) {
        return players.containsKey(playerId);
    }

    /**
     * Runs on the world map thread: never reads a component, only schedules a recompute and joins
     * its result with the positions, which are safe to read from any thread. The server hands a
     * {@code Player} alone, whose identity getters are all marked for removal.
     */
    @SuppressWarnings("removal")
    @Override
    public void update(@Nonnull World world, @Nonnull Player player, @Nonnull MarkersCollector collector) {
        UUID playerId = player.getPlayerRef().getUuid();
        PlayerMarkers markers = players.computeIfAbsent(playerId, id -> new PlayerMarkers());
        Seen seen = markers.seen;
        boolean here = seen != null && seen.world.equals(world.getWorldConfig().getUuid());
        if ((!here || markers.stale) && markers.pending.compareAndSet(false, true)) world.execute(() -> recompute(world, playerId, markers));
        if (!here || seen.positions == null) return;

        // Read before the view, so a change in between only makes the next call join again
        int version = seen.positions.version();
        Built built = markers.built;
        if (built == null || built.from != seen || built.version != version) {
            built = join(seen, version, seen.positions.view());
            markers.built = built;
            if (built.undecided) markers.stale = true;
        }
        built.replay(markers.mapOpen, collector);
    }

    private void recompute(@Nonnull World world, @Nonnull UUID playerId, @Nonnull PlayerMarkers markers) {
        UUID worldId = world.getWorldConfig().getUuid();
        try {
            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef == null) {
                // A call that came after the player left must not keep them
                players.remove(playerId, markers);
                return;
            }
            if (!worldId.equals(playerRef.getWorldUuid())) return;
            markers.stale = false;
            CharacterPositions positions = this.positions.positions(world.getEntityStore().getStore());
            Set<String> decided = Set.copyOf(positions.view().keySet());
            DialogueHistory history = histories.apply(playerRef);
            Seen seen = new Seen(worldId, positions, decided, looks(decided, id -> history != null && history.count(id, null, null) > 0, statuses(playerId)));
            if (players.get(playerId) == markers) markers.seen = seen;
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Could not compute the character markers of player %s.", playerId);
            markers.seen = new Seen(worldId, null, Set.of(), Map.of());
        } finally {
            markers.pending.set(false);
        }
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
            if (shown != null) looks.put(characterId, new Look(shown, character.getEpithet()));
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
        boolean undecided = false;
        for (Map.Entry<String, Collection<CharacterPositions.Spot>> entry : spots.entrySet()) {
            Look look = seen.shown.get(entry.getKey());
            if (look == null) {
                undecided |= !seen.decided.contains(entry.getKey());
                continue;
            }
            for (CharacterPositions.Spot spot : entry.getValue()) {
                MapMarker marker = marker(spot, look.shown(), spot.name() != null ? spot.name() : look.epithet());
                all.add(marker);
                if (look.shown().onCompass()) compass.add(marker);
            }
        }
        return new Built(seen, version, List.copyOf(all), List.copyOf(compass), undecided);
    }

    @Nonnull
    private static MapMarker marker(@Nonnull CharacterPositions.Spot spot, @Nonnull CharacterMarkerRule.Shown shown, @Nullable String name) {
        MapMarkerBuilder builder = new MapMarkerBuilder("OpenDialogues:" + spot.npc(), shown.image(), new Transform(spot.position()));
        if (name != null) builder.withName(Message.translation(name));
        if (shown.onCompass()) builder.withCompassImage(shown.image());
        return builder.build();
    }
}
