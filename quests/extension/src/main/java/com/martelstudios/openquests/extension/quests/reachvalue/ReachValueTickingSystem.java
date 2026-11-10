package com.martelstudios.openquests.extension.quests.reachvalue;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;

/**
 * Reads the watched values every tick, since none of them fires an event. Also covers a quest
 * given after its value was reached: the next tick completes it.
 */
public class ReachValueTickingSystem extends EntityTickingSystem<EntityStore> {

    private final QuestProgressionService progression;

    public ReachValueTickingSystem(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), ReachValueQuestListener.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var listener = archetypeChunk.getComponent(index, ReachValueQuestListener.getComponentType());
        if (playerRef == null || listener == null) return;

        var ref = archetypeChunk.getReferenceTo(index);
        progression.progress(new ReachValueQuestVisitor(playerRef.getUuid(), source -> source.read(ref, store)), listener.getQuestIds());
    }
}
