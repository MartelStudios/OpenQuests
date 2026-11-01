package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseEntityEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Pressing F on an NPC carrying a character lets its traits answer in place of the NPC's own use,
 * which a passive NPC would refuse anyway.
 */
public class CharacterTalkSystem extends EntityEventSystem<EntityStore, UseEntityEvent.Pre> {

    private final CharacterService service;
    private final ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType;
    private final ResourceType<EntityStore, UniqueCharacters> uniqueType;

    public CharacterTalkSystem(@Nonnull CharacterService service, @Nonnull ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType, @Nonnull ResourceType<EntityStore, UniqueCharacters> uniqueType) {
        super(UseEntityEvent.Pre.class);
        this.service = service;
        this.identityType = identityType;
        this.uniqueType = uniqueType;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull UseEntityEvent.Pre event) {
        Ref<EntityStore> target = event.getTargetEntity();
        if (!target.isValid() || store.getComponent(target, identityType) == null) return;
        PlayerRef player = store.getComponent(archetypeChunk.getReferenceTo(index), PlayerRef.getComponentType());
        CharacterNpc npc = CharacterNpc.of(commandBuffer, target, identityType, uniqueType);
        if (player != null && npc != null && service.use(npc, player)) event.setCancelled(true);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
