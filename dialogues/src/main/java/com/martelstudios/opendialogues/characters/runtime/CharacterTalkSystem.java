package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.UseEntityEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.OpenDialogueCharacterAsset;
import com.martelstudios.opendialogues.characters.CharacterAssets;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;
import com.martelstudios.opendialogues.characters.traits.CharacterTrait;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pressing F on an NPC carrying a character opens its dialogue in place of the NPC's own use,
 * which a passive NPC would refuse anyway.
 */
public class CharacterTalkSystem extends EntityEventSystem<EntityStore, UseEntityEvent.Pre> {

    private final ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType;

    public CharacterTalkSystem(@Nonnull ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType) {
        super(UseEntityEvent.Pre.class);
        this.identityType = identityType;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull UseEntityEvent.Pre event) {
        Ref<EntityStore> target = event.getTargetEntity();
        if (!target.isValid()) return;
        OpenDialoguesNPCIdentity identity = store.getComponent(target, identityType);
        if (identity == null || identity.getCharacter() == null) return;
        OpenDialogueCharacterAsset character = OpenDialogueCharacterAsset.getCharacter(identity.getCharacter());
        PlayerRef player = store.getComponent(archetypeChunk.getReferenceTo(index), PlayerRef.getComponentType());
        UUID npc = CharacterScanSystem.uuidOf(store, target);
        if (character == null || player == null || npc == null) return;

        CharacterContext context = new CharacterContext(store, target, npc, identity, character, CharacterAssets.live(), ThreadLocalRandom.current());
        for (CharacterTrait trait : character.getTraits()) {
            if (trait.onUse(context, player)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
