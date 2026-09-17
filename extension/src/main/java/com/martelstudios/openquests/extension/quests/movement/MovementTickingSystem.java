package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;

/**
 * Samples how each connected player is moving, once per tick, and hands it to their movement
 * quests. Distance is integrated from the velocity rather than measured between two positions:
 * a speed is already there to be read, where a previous position would have to be kept per player
 * and given back when they leave.
 */
public class MovementTickingSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), MovementStatesComponent.getComponentType(), QuestStoreComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var movementStates = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());
        var questStoreComponent = archetypeChunk.getComponent(index, QuestStoreComponent.getComponentType());

        if (playerRef == null || movementStates == null || questStoreComponent == null) return;

        MovementStates states = movementStates.getMovementStates();
        if (states == null) return;

        // Read rather than queried: a player who somehow carries no velocity still has their jumps
        // counted, where asking for it up front would drop them from the pass altogether
        var velocity = archetypeChunk.getComponent(index, Velocity.getComponentType());
        double metres = velocity == null ? 0 : horizontalSpeed(velocity) * dt;

        QuestProgressionService.get()
                               .progress(new MovementQuestVisitor(playerRef.getUuid(), states, metres), questStoreComponent.getQuestIds());
    }

    /**
     * Flat distance only: a quest asking for a hundred metres means across the ground, and counting
     * the climb would have a ladder pay the same as a road.
     */
    private static double horizontalSpeed(@Nonnull Velocity velocity) {
        double x = velocity.getX();
        double z = velocity.getZ();

        return Math.sqrt(x * x + z * z);
    }
}
