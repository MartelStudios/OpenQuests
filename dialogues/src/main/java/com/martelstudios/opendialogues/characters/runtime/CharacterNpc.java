package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An NPC as the character service works on it: its identity is a copy, which the service saves
 * through {@code save} once a call changed it.
 *
 * @param accessor the store or command buffer the NPC is read and written through, {@code null} in unit tests.
 * @param unique   the unique characters as the NPC's world sees them, its own and the universe's.
 * @param world    the NPC's world registry alone, whose reset generations tell a stale NPC.
 * @param save     writes the identity back on the NPC.
 */
record CharacterNpc(@Nullable ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref, @Nonnull UUID uuid, @Nonnull OpenDialoguesNPCIdentity identity, @Nonnull CharacterClaims unique, @Nonnull UniqueCharacters world, @Nonnull Consumer<OpenDialoguesNPCIdentity> save) {

    /**
     * @param scopes finds a character to read its scope.
     * @return the NPC, a fresh identity when it has none yet; {@code null} for an entity without UUID.
     */
    @Nullable
    static CharacterNpc of(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref, @Nonnull ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType, @Nonnull ResourceType<EntityStore, UniqueCharacters> uniqueType, @Nonnull UniverseCharacters universe, @Nonnull Function<String, OpenDialogueCharacterAsset> scopes) {
        UUIDComponent uuid = accessor.getComponent(ref, UUIDComponent.getComponentType());
        if (uuid == null) return null;
        OpenDialoguesNPCIdentity stored = accessor.getComponent(ref, identityType);
        OpenDialoguesNPCIdentity identity = stored == null ? new OpenDialoguesNPCIdentity() : (OpenDialoguesNPCIdentity) stored.clone();
        UUID worldId = accessor.getExternalData().getWorld().getWorldConfig().getUuid();
        UniqueCharacters world = accessor.getResource(uniqueType);
        CharacterClaims claims = new ScopedClaims(world, universe, worldId, scopes);
        return new CharacterNpc(accessor, ref, uuid.getUuid(), identity, claims, world, changed -> accessor.putComponent(ref, identityType, changed));
    }
}
