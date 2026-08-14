package com.martelstudios.hyquests.extension.reachlocation;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.core.services.QuestProgressionService;
import com.martelstudios.hyquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;

/**
 * Checks every connected player's position against their {@link com.martelstudios.hyquests.core.models.ReachLocationQuest}s
 * once per tick. Simpler than Hytale's marker + spatial-index approach — fine at this mod's scale
 * since each check is just a distance comparison against a bounded set of quests.
 */
public class ReachLocationTickingSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), TransformComponent.getComponentType(), QuestStoreComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        var questStoreComponent = archetypeChunk.getComponent(index, QuestStoreComponent.getComponentType());

        if (playerRef == null || transform == null || questStoreComponent == null) return;

        QuestProgressionService.get()
                               .progress(new ReachLocationQuestVisitor(playerRef.getUuid(), transform.getPosition()), questStoreComponent.questsRecord.getAllIds());
    }
}
