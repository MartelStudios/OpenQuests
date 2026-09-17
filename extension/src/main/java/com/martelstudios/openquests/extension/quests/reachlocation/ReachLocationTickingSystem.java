package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Checks a player's position against the reach-location quests they are running, once per tick.
 * Simpler than Hytale's marker + spatial-index approach — fine at this mod's scale, since the query
 * leaves out every player who has no such quest and each check is a distance comparison.
 */
public class ReachLocationTickingSystem extends EntityTickingSystem<EntityStore> {
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

        QuestProgressionService.get()
                               .progress(new ReachLocationQuestVisitor(playerRef.getUuid(), transform.getPosition()), listener.getQuestIds());
    }
}
