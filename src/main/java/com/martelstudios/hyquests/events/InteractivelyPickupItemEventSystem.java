package com.martelstudios.hyquests.events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.services.QuestProgressionService;
import com.martelstudios.hyquests.visitors.InteractivelyPickupQuestVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InteractivelyPickupItemEventSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {
    public InteractivelyPickupItemEventSystem() {
        super(InteractivelyPickupItemEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractivelyPickupItemEvent event) {
        var ref = chunk.getReferenceTo(index);

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        QuestProgressionService.get().progress(new InteractivelyPickupQuestVisitor(playerRef.getUuid(), event));
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
