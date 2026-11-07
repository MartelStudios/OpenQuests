package com.martelstudios.opendialogues.map;

import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.runtime.CharacterService;
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

/**
 * Puts the NPCs carrying a character on each player's map, and on their compass when another mod
 * says so. Computes rarely, on the player's world thread, and replays the result on every call.
 */
public final class CharacterMarkerProvider implements WorldMapManager.MarkerProvider {

    /** Id under which every world registers the provider. */
    public static final String ID = "OpenDialoguesCharacters";

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * A player's markers, as computed in one world from one version of its positions.
     */
    record Built(@Nonnull String world, int version, @Nonnull List<MapMarker> all, @Nonnull List<MapMarker> compass) {

        /**
         * A closed map shows nothing but the compass, which shows every marker it is given.
         */
        void replay(boolean mapOpen, @Nonnull MarkersCollector collector) {
            for (MapMarker marker : mapOpen ? all : compass) collector.addIgnoreViewDistance(marker);
        }
    }

    private final ResourceType<EntityStore, CharacterPositions> positionsType;
    private final CharacterService characters;
    private final Function<PlayerRef, DialogueHistory> histories;
    private final Function<String, OpenDialogueCharacterAsset> assets;
    private final List<CharacterStatusProvider> statusProviders = new CopyOnWriteArrayList<>();
    private final Map<UUID, Built> built = new ConcurrentHashMap<>();
    private final Map<String, CharacterPositions> positionsByWorld = new ConcurrentHashMap<>();
    private final Set<UUID> stale = ConcurrentHashMap.newKeySet();
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
    private final Set<UUID> mapOpen = ConcurrentHashMap.newKeySet();

    /**
     * @param histories a player's dialogue history, read on their world thread, {@code null} elsewhere.
     * @param assets    finds a loaded character by id, {@code null} for one no pack declares.
     */
    public CharacterMarkerProvider(@Nonnull ResourceType<EntityStore, CharacterPositions> positionsType, @Nonnull CharacterService characters, @Nonnull Function<PlayerRef, DialogueHistory> histories, @Nonnull Function<String, OpenDialogueCharacterAsset> assets) {
        this.positionsType = positionsType;
        this.characters = characters;
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
     * Has the player's markers computed again, as what decides them changed. Any thread.
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
        pending.remove(playerId);
        mapOpen.remove(playerId);
    }

    /**
     * @return a world's positions, first forgetting those a reset made stale; on the world thread.
     */
    @Nonnull
    public CharacterPositions positions(@Nonnull Store<EntityStore> store) {
        CharacterPositions positions = store.getResource(positionsType);
        positions.catchUp(characters.universeRegistry().generation(), characters.worldRegistry(store).generation());
        return positions;
    }

    /**
     * Runs on the world map thread: never reads a component, only schedules a recompute. The
     * server hands a {@code Player} alone, whose identity getters are all marked for removal.
     */
    @SuppressWarnings("removal")
    @Override
    public void update(@Nonnull World world, @Nonnull Player player, @Nonnull MarkersCollector collector) {
        UUID playerId = player.getPlayerRef().getUuid();
        Built current = built.get(playerId);
        boolean here = current != null && current.world.equals(world.getName());
        CharacterPositions positions = positionsByWorld.get(world.getName());
        boolean fresh = here && positions != null && current.version == positions.version() && !stale.contains(playerId);
        if (!fresh && pending.add(playerId)) world.execute(() -> recompute(world, playerId));
        if (here) current.replay(mapOpen.contains(playerId), collector);
    }

    /**
     * A failure leaves an empty list for the current positions, so it is not retried ten times a
     * second until something changes.
     */
    private void recompute(@Nonnull World world, @Nonnull UUID playerId) {
        int version = -1;
        try {
            PlayerRef playerRef = Universe.get().getPlayer(playerId);
            if (playerRef == null) return;
            if (!world.getWorldConfig().getUuid().equals(playerRef.getWorldUuid())) return;
            stale.remove(playerId);
            CharacterPositions positions = positions(world.getEntityStore().getStore());
            positionsByWorld.put(world.getName(), positions);
            version = positions.version();
            built.put(playerId, build(world.getName(), version, positions.view(), histories.apply(playerRef), statuses(playerId)));
        } catch (RuntimeException e) {
            LOGGER.atWarning().withCause(e).log("Could not compute the character markers of player %s.", playerId);
            built.put(playerId, new Built(world.getName(), version, List.of(), List.of()));
        } finally {
            pending.remove(playerId);
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

    @Nonnull
    Built build(@Nonnull String world, int version, @Nonnull Map<String, Collection<CharacterPositions.Spot>> spots, @Nullable DialogueHistory history, @Nonnull Map<String, List<CharacterMapStatus>> statuses) {
        List<MapMarker> all = new ArrayList<>();
        List<MapMarker> compass = new ArrayList<>();
        spots.forEach((characterId, npcs) -> {
            OpenDialogueCharacterAsset character = assets.apply(characterId);
            if (character == null) return;
            boolean met = history != null && history.count(characterId, null, null) > 0;
            CharacterMarkerRule.Shown shown = CharacterMarkerRule.resolve(character.getMapVisibility(), character.getMapIcon(), met, CharacterMarkerRule.merge(statuses.getOrDefault(characterId, List.of())));
            if (shown == null) return;
            for (CharacterPositions.Spot spot : npcs) {
                MapMarker marker = marker(spot, shown, spot.name() != null ? spot.name() : character.getEpithet());
                all.add(marker);
                if (shown.onCompass()) compass.add(marker);
            }
        });
        return new Built(world, version, List.copyOf(all), List.copyOf(compass));
    }

    @Nonnull
    private static MapMarker marker(@Nonnull CharacterPositions.Spot spot, @Nonnull CharacterMarkerRule.Shown shown, @Nullable String name) {
        MapMarkerBuilder builder = new MapMarkerBuilder("OpenDialogues:" + spot.npc(), shown.image(), new Transform(spot.position()));
        if (name != null) builder.withName(Message.translation(name));
        if (shown.onCompass()) builder.withCompassImage(shown.image());
        return builder.build();
    }
}
