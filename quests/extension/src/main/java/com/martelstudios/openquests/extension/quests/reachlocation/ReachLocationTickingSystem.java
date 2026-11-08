package com.martelstudios.openquests.extension.quests.reachlocation;

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
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Checks a player's position against the reach-location and path quests they are running, once per
 * tick, then shows each running quest's marker where it now points.
 * Simpler than Hytale's marker + spatial-index approach, and fine at this mod's scale, since the query
 * leaves out every player who has no such quest and each check is a distance comparison.
 */
public class ReachLocationTickingSystem extends EntityTickingSystem<EntityStore> {

    private final QuestProgressionService progression;
    private final QuestMarkers markers;

    public ReachLocationTickingSystem(@Nonnull QuestProgressionService progression, @Nonnull QuestMarkers markers) {
        this.progression = progression;
        this.markers = markers;
    }
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), TransformComponent.getComponentType(), ReachLocationQuestListener.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        var listener = archetypeChunk.getComponent(index, ReachLocationQuestListener.getComponentType());

        if (playerRef == null || transform == null || listener == null) return;

        var movementStates = archetypeChunk.getComponent(index, MovementStatesComponent.getComponentType());
        MovementStates states = movementStates == null ? null : movementStates.getMovementStates();

        progression.progress(new ReachLocationQuestVisitor(playerRef.getUuid(), transform.getPosition()), listener.getQuestIds());
        progression.progress(new ReachPathQuestVisitor(playerRef, transform.getPosition(), states != null && states.onGround), listener.getQuestIds());

        for (UUID questId : listener.getQuestIds()) {
            showMarker(playerRef, progression.getLiveQuest(questId));
        }
    }

    /**
     * Quests that ended are hidden by {@link QuestMarkers#hide}, called as they end.
     */
    private void showMarker(@Nonnull PlayerRef playerRef, AbstractQuestProgression<?> quest) {
        if (quest == null || quest.isOver()) return;

        if (quest instanceof ReachLocationQuestProgression reach && reach.getAsset() != null) {
            markers.show(playerRef, quest.getId(), reach.getAsset().getMarker(), reach.getAsset().getPosition());
        } else if (quest instanceof ReachPathQuestProgression path && path.getAsset() != null) {
            int next = path.getCurrentQuantity();
            markers.show(playerRef, quest.getId(), path.getAsset().markerFor(next), path.getAsset().getPosition(next));
        }
    }
}
