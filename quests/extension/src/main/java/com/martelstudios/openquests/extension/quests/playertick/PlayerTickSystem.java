package com.martelstudios.openquests.extension.quests.playertick;

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
 * Collects the tick duration, movement state and distance travelled once per player tick.
 * Players enter the query when they have a quest listening for these samples.
 */
public class PlayerTickSystem extends EntityTickingSystem<EntityStore> {

    private final QuestProgressionService progression;

    public PlayerTickSystem(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), TransformComponent.getComponentType(), MovementStatesComponent.getComponentType(), PlayerTickQuestListener.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        var movementStates = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());
        var listener = archetypeChunk.getComponent(index, PlayerTickQuestListener.getComponentType());

        if (playerRef == null || transform == null || movementStates == null || listener == null) return;

        MovementStates states = movementStates.getMovementStates();
        if (states == null) return;

        var position = transform.getPosition();

        // Sampled whatever the gait, so that the tick a player changes pace on is measured from
        // where they were rather than from wherever they last happened to be counted
        double metres = listener.sampleTravel(position.x(), position.z());

        progression.progress(new PlayerTickQuestVisitor(playerRef.getUuid(), states, metres, dt), listener.getQuestIds());
    }
}
