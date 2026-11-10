package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.packets.world.CancelParticleSystems;
import com.hypixel.hytale.protocol.packets.world.SpawnParticleSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The particle markers each player is shown for their quests, sent to that player alone. A marker
 * is only sent again when it appears, when the player changed world, which clears what their
 * client drew, or when the player comes back within {@link #RANGE} of it.
 */
public final class QuestMarkers {

    /** The client drops a particle system spawned farther away than this (seen between 30 and 40 blocks). */
    private static final double RANGE = 30;

    private final Map<UUID, Map<UUID, Set<Marker>>> shown = new ConcurrentHashMap<>();

    /**
     * Shows a player standing at {@code at} the quest's markers near them, taking away the others.
     */
    public void show(@Nonnull PlayerRef player, @Nonnull Vector3d at, @Nonnull UUID questId, @Nonnull Set<Marker> wanted) {
        Map<UUID, Set<Marker>> quests = shown.computeIfAbsent(player.getUuid(), id -> new ConcurrentHashMap<>());
        UUID world = player.getWorldUuid();
        Set<Marker> next = new HashSet<>();
        for (Marker marker : wanted) {
            if (marker.position().distanceSquared(at) <= RANGE * RANGE) next.add(marker.in(world));
        }

        Set<Marker> before = quests.getOrDefault(questId, Set.of());
        if (next.equals(before)) return;

        for (Marker marker : before) {
            // A marker left in another world went with that world's particles
            if (!next.contains(marker) && marker.world() != null && marker.world().equals(world)) cancel(player, marker);
        }
        for (Marker marker : next) {
            if (!before.contains(marker)) spawn(player, marker);
        }
        if (next.isEmpty()) quests.remove(questId); else quests.put(questId, next);
    }

    /**
     * Takes a quest's markers away from everyone shown them, for a quest that is over.
     */
    public void hide(@Nonnull UUID questId) {
        shown.forEach((playerId, quests) -> {
            Set<Marker> markers = quests.remove(questId);
            PlayerRef player = markers == null ? null : Universe.get().getPlayer(playerId);
            if (player != null) markers.forEach(marker -> cancel(player, marker));
        });
    }

    /**
     * Drops what a player was shown: a client that reconnects starts with no particle.
     */
    public void forget(@Nonnull UUID playerId) {
        shown.remove(playerId);
    }

    private static void spawn(@Nonnull PlayerRef player, @Nonnull Marker marker) {
        Vector3d at = marker.position();
        player.getPacketHandler().write(new SpawnParticleSystem(marker.particle(), new Position(at.x, at.y, at.z), new Direction(0, 0, 0), 1, null, 0));
    }

    private static void cancel(@Nonnull PlayerRef player, @Nonnull Marker marker) {
        Vector3d at = marker.position();
        player.getPacketHandler().write(new CancelParticleSystems(new Position(at.x - 0.5, at.y - 0.5, at.z - 0.5), new Position(at.x + 0.5, at.y + 0.5, at.z + 0.5), new String[]{marker.particle()}, false));
    }

    /**
     * A particle system shown at a position, in the world it was sent to.
     */
    public record Marker(@Nonnull String particle, @Nonnull Vector3d position, @Nullable UUID world) {

        /**
         * @return a marker for that spot, or {@code null} for a spot that has none.
         */
        @Nullable
        public static Marker of(@Nonnull ReachLocationQuestAsset spot) {
            return spot.getMarker() == null || spot.getPosition() == null ? null : new Marker(spot.getMarker(), new Vector3d(spot.getPosition()), null);
        }

        private Marker in(@Nullable UUID world) {
            return new Marker(particle, position, world);
        }
    }
}
