package com.martelstudios.openquests.extension.quests.useentity;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseEntityEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The event is published on the interacting entity, so the used one is read from the event.
 */
public class UseEntityEventSystem extends EntityEventSystem<EntityStore, UseEntityEvent.Post> {
    public UseEntityEventSystem() {
        super(UseEntityEvent.Post.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull UseEntityEvent.Post event) {
        var ref = chunk.getReferenceTo(index);

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var questStoreComponent = store.getComponent(ref, QuestStoreComponent.getComponentType());
        if (playerRef == null || questStoreComponent == null) return;

        var targetRef = event.getTargetEntity();
        if (!targetRef.isValid()) return;

        var target = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (target == null) return;

        QuestProgressionService.get()
                               .progress(new UseEntityQuestVisitor(playerRef.getUuid(), target), questStoreComponent.questsRecord.getAllIds());
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), QuestStoreComponent.getComponentType());
    }
}
