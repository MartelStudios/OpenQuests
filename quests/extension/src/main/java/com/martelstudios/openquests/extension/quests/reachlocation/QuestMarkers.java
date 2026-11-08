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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The particle markers each player is shown, one per quest, sent to that player alone. A marker is
 * only sent again when it moves, or when the player changed world, which clears what their client
 * drew.
 */
public final class QuestMarkers {

    private final Map<UUID, Map<UUID, Shown>> shown = new ConcurrentHashMap<>();

    /**
     * Shows a quest's marker to a player, moving it if it stood elsewhere.
     *
     * @param particle the particle system to show, or {@code null} to show nothing for that quest.
     */
    public void show(@Nonnull PlayerRef player, @Nonnull UUID questId, @Nullable String particle, @Nullable Vector3d position) {
        Map<UUID, Shown> markers = shown.computeIfAbsent(player.getUuid(), id -> new ConcurrentHashMap<>());
        Shown before = markers.get(questId);

        if (particle == null || position == null) {
            if (before != null) cancel(player, markers.remove(questId));
            return;
        }

        Shown next = new Shown(particle, new Vector3d(position), player.getWorldUuid());
        if (next.equals(before)) return;

        // A marker left in another world went with that world's particles
        if (before != null && before.world().equals(next.world())) cancel(player, before);
        markers.put(questId, next);
        player.getPacketHandler().write(new SpawnParticleSystem(particle, new Position(position.x, position.y, position.z), new Direction(0, 0, 0), 1, null, 0));
    }

    /**
     * Takes a quest's marker away from everyone shown it, for a quest that is over.
     */
    public void hide(@Nonnull UUID questId) {
        shown.forEach((playerId, markers) -> {
            Shown marker = markers.remove(questId);
            PlayerRef player = marker == null ? null : Universe.get().getPlayer(playerId);
            if (player != null) cancel(player, marker);
        });
    }

    /**
     * Drops what a player was shown: a client that reconnects starts with no particle.
     */
    public void forget(@Nonnull UUID playerId) {
        shown.remove(playerId);
    }

    private static void cancel(@Nonnull PlayerRef player, @Nonnull Shown marker) {
        Vector3d at = marker.position();
        player.getPacketHandler().write(new CancelParticleSystems(new Position(at.x - 0.5, at.y - 0.5, at.z - 0.5), new Position(at.x + 0.5, at.y + 0.5, at.z + 0.5), new String[]{marker.particle()}, false));
    }

    private record Shown(@Nonnull String particle, @Nonnull Vector3d position, @Nullable UUID world) {}
}
