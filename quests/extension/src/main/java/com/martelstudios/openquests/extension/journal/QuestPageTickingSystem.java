package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Keeps the countdowns of an open journal running. Asked on the player's own thread, where the page
 * can redraw itself, and it is the page that decides whether a second has passed: a player without
 * the journal open costs one lookup.
 */
public class QuestPageTickingSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), Player.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var player = archetypeChunk.getComponent(index, Player.getComponentType());
        if (player == null || !(player.getPageManager().getCustomPage() instanceof QuestPage page)) return;

        page.tickCountdowns(archetypeChunk.getReferenceTo(index));
    }
}
