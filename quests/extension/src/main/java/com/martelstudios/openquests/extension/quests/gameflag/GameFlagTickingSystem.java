package com.martelstudios.openquests.extension.quests.gameflag;

import com.hypixel.hytale.builtin.gameflags.GameFlagsResource;
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
 * Reads the flags every tick, since raising one fires no event. Also covers a quest assigned after
 * its flag was raised: the next tick completes it.
 */
public class GameFlagTickingSystem extends EntityTickingSystem<EntityStore> {

    private final QuestProgressionService progression;

    public GameFlagTickingSystem(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), GameFlagQuestListener.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var listener = archetypeChunk.getComponent(index, GameFlagQuestListener.getComponentType());
        if (playerRef == null || listener == null) return;

        GameFlagsResource flags = GameFlagsResource.get();
        progression.progress(new GameFlagQuestVisitor(playerRef.getUuid(), flags::getLevel), listener.getQuestIds());
    }
}
