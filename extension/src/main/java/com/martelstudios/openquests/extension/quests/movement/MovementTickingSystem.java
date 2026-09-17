package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Samples how a player is moving, once per tick, and hands it to the movement quests they are
 * running. Only players with one are in the query at all, and only those quests are walked.
 *
 * <p>Distance is the ground actually covered between two ticks. Owing nothing to the units a
 * velocity is expressed in, nor to which of the two a server holds for a player is the one driving
 * them, it is the only measure that answers in blocks without being told the scale.
 */
public class MovementTickingSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), TransformComponent.getComponentType(), MovementStatesComponent.getComponentType(), MovementQuestListener.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        var movementStates = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());
        var listener = archetypeChunk.getComponent(index, MovementQuestListener.getComponentType());

        if (playerRef == null || transform == null || movementStates == null || listener == null) return;

        MovementStates states = movementStates.getMovementStates();
        if (states == null) return;

        var position = transform.getPosition();

        // Sampled whatever the gait, so that the tick a player changes pace on is measured from
        // where they were rather than from wherever they last happened to be counted
        double metres = listener.sampleTravel(position.x(), position.z());

        QuestProgressionService.get()
                               .progress(new MovementQuestVisitor(playerRef.getUuid(), states, metres), listener.getQuestIds());
    }
}
