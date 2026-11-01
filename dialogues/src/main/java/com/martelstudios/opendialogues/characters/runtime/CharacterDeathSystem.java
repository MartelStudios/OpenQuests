package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.ReleaseReason;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Frees the unique character a dying NPC carried, the rest of its identity dying with it.
 */
public class CharacterDeathSystem extends DeathSystems.OnDeathSystem {

    private final ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType;
    private final ResourceType<EntityStore, UniqueCharacters> uniqueType;

    public CharacterDeathSystem(@Nonnull ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType, @Nonnull ResourceType<EntityStore, UniqueCharacters> uniqueType) {
        this.identityType = identityType;
        this.uniqueType = uniqueType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return identityType;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        UUID npc = CharacterScanSystem.uuidOf(store, ref);
        if (npc != null) store.getResource(uniqueType).release(npc, ReleaseReason.Death, OpenDialogueCharacterAsset::getCharacter);
    }
}
