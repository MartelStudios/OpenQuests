package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.ReleaseReason;

import javax.annotation.Nonnull;

/**
 * Releases the character a dying NPC carried, through the command buffer since the store is ticking.
 */
public class CharacterDeathSystem extends DeathSystems.OnDeathSystem {

    private final CharacterService service;
    private final ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType;

    public CharacterDeathSystem(@Nonnull CharacterService service, @Nonnull ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType) {
        this.service = service;
        this.identityType = identityType;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return identityType;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull DeathComponent component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        service.edit(commandBuffer, ref, npc -> {
            service.release(npc, ReleaseReason.Death);
            return null;
        });
    }
}
