package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hands out a world's {@link CharacterPositions} caught up with character resets, so no reader
 * shows NPCs a reset sent back to drawing.
 */
public final class CharacterPositionsAccess {

    private final ResourceType<EntityStore, CharacterPositions> type;
    private final CharacterService characters;

    /**
     * @param characters the service whose registries count the resets.
     */
    public CharacterPositionsAccess(@Nonnull ResourceType<EntityStore, CharacterPositions> type, @Nonnull CharacterService characters) {
        this.type = type;
        this.characters = characters;
    }

    /**
     * @return a world's positions, first forgetting those a reset made stale; on the world thread.
     */
    @Nonnull
    public CharacterPositions positions(@Nonnull Store<EntityStore> store) {
        CharacterPositions positions = store.getResource(type);
        positions.catchUp(characters.universeRegistry().generation(), characters.worldRegistry(store).generation());
        return positions;
    }

    /**
     * A world's NPCs per character, readable from any thread.
     * shortcut: skips the reset catch-up, so a reset's NPCs linger until the next scan; catch up here if that shows.
     */
    @Nonnull
    public Map<String, Collection<CharacterPositions.Spot>> view(@Nonnull ComponentAccessor<EntityStore> store) {
        return store.getResource(type).view();
    }

    /**
     * Records where an NPC carrying a character stands, so its marker follows it; on the world thread.
     */
    void record(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull CharacterNpc npc) {
        String characterId = npc.identity().getCharacter();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (characterId == null || transform == null) return;
        positions(store).record(characterId, npc.uuid(), transform.getPosition(), characters.displayName(npc.identity()));
    }

    /**
     * @return every NPC carrying a character in a world, loaded or not, readable from any thread.
     */
    @Nonnull
    public Set<UUID> npcs(@Nonnull ComponentAccessor<EntityStore> store) {
        Set<UUID> npcs = new HashSet<>();
        for (Collection<CharacterPositions.Spot> spots : view(store).values()) {
            for (CharacterPositions.Spot spot : spots) npcs.add(spot.npc());
        }
        return npcs;
    }
}
