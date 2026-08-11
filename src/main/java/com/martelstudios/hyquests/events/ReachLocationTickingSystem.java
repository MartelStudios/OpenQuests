package com.martelstudios.hyquests.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.services.QuestService;
import com.martelstudios.hyquests.visitors.ReachLocationQuestVisitor;

import javax.annotation.Nonnull;

/**
 * Checks every connected player's position against their {@link com.martelstudios.hyquests.models.ReachLocationQuest}s
 * once per tick. Simpler than Hytale's marker + spatial-index approach — fine at this mod's scale
 * since each check is just a distance comparison against a bounded set of quests.
 */
public class ReachLocationTickingSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());

        if (playerRef == null || transform == null) return;

        QuestService.get().progress(new ReachLocationQuestVisitor(playerRef.getUuid(), transform.getPosition()));
    }
}
