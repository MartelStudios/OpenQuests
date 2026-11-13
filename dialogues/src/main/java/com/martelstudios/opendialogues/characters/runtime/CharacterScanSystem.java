package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Every few seconds, finds the NPCs carrying a character near each player of a world: their traits
 * are applied again and their position recorded. Draws happen at spawn, in {@link CharacterSpawnSystem}.
 */
public class CharacterScanSystem extends DelayedSystem<EntityStore> {

    private final double radius;
    private final CharacterService service;
    private final CharacterPositionsAccess positions;

    /**
     * @param positions where each visit records its NPC.
     */
    public CharacterScanSystem(@Nonnull Supplier<CharacterSettings> settings, @Nonnull CharacterService service, @Nonnull CharacterPositionsAccess positions) {
        super(settings.get().getScanInterval());
        this.radius = settings.get().getScanRadius();
        this.service = service;
        this.positions = positions;
    }

    /**
     * Deferred past the tick, during which the store refuses the components traits add.
     */
    @Override
    public void delayedTick(float dt, int systemIndex, @Nonnull Store<EntityStore> store) {
        World world = store.getExternalData().getWorld();
        if (world.getPlayerRefs().isEmpty()) return;
        world.execute(() -> scan(world, store));
    }

    private void scan(@Nonnull World world, @Nonnull Store<EntityStore> store) {
        Set<Ref<EntityStore>> seen = new HashSet<>();
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            Ref<EntityStore> playerEntity = playerRef.getReference();
            if (playerEntity == null || !playerEntity.isValid()) continue;
            TransformComponent transform = store.getComponent(playerEntity, TransformComponent.getComponentType());
            if (transform == null) continue;

            List<Ref<EntityStore>> nearby = SpatialResource.getThreadLocalReferenceList();
            store.getResource(NPCPlugin.get().getNpcSpatialResource()).getSpatialStructure().collect(transform.getPosition(), radius, nearby);
            for (Ref<EntityStore> npc : List.copyOf(nearby)) {
                if (npc.isValid() && seen.add(npc) && service.carriesCharacter(store, npc)) visit(store, npc);
            }
        }
    }

    private void visit(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        service.edit(store, ref, npc -> {
            service.apply(npc);
            positions.record(store, ref, npc);
            return null;
        });
    }
}
