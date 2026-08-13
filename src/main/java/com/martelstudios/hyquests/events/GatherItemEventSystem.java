package com.martelstudios.hyquests.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.services.QuestProgressionService;
import com.martelstudios.hyquests.visitors.GatherQuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Re-checks {@link com.martelstudios.hyquests.models.GatherQuest} progress whenever a
 * player's inventory changes, regardless of how the item got there (unlike interactive-pickup
 * quests, this isn't tied to a specific harvest/pickup interaction).
 */
public class GatherItemEventSystem extends EntityEventSystem<EntityStore, InventoryChangeEvent> {
    public GatherItemEventSystem() {
        super(InventoryChangeEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryChangeEvent event) {
        var ref = chunk.getReferenceTo(index);

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        QuestProgressionService.get().progress(new GatherQuestVisitor(playerRef.getUuid(), ref, store));
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
