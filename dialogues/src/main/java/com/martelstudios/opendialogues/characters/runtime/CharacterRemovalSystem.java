package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.ReleaseReason;

import javax.annotation.Nonnull;

/**
 * Releases the character of an NPC removed for good without dying, a command or a despawn. A chunk
 * unload is no release: the NPC comes back with its character. Nothing is saved on the leaving entity.
 */
public class CharacterRemovalSystem extends RefSystem<EntityStore> {

    private final CharacterService service;
    private final ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType;
    private final ResourceType<EntityStore, UniqueCharacters> uniqueType;

    public CharacterRemovalSystem(@Nonnull CharacterService service, @Nonnull ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType, @Nonnull ResourceType<EntityStore, UniqueCharacters> uniqueType) {
        this.service = service;
        this.identityType = identityType;
        this.uniqueType = uniqueType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return identityType;
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {}

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (reason == RemoveReason.UNLOAD) return;
        CharacterNpc npc = CharacterNpc.of(commandBuffer, ref, identityType, uniqueType);
        if (npc != null) service.release(npc, ReleaseReason.Removed);
    }
}
