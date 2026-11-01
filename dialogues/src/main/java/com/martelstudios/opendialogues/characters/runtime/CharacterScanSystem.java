package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldMapTracker;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Every few seconds, finds the NPCs near each player of a world: those carrying a character get
 * their traits applied again, the others may draw one. The character service does the rest.
 */
public class CharacterScanSystem extends DelayedSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final double radius;
    private final CharacterService service;

    public CharacterScanSystem(@Nonnull CharacterSettings settings, @Nonnull CharacterService service) {
        super(settings.getScanInterval());
        this.radius = settings.getScanRadius();
        this.service = service;
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
            Player player = store.getComponent(playerEntity, Player.getComponentType());
            if (transform == null || player == null) continue;

            List<Ref<EntityStore>> nearby = SpatialResource.getThreadLocalReferenceList();
            store.getResource(NPCPlugin.get().getNpcSpatialResource()).getSpatialStructure().collect(transform.getPosition(), radius, nearby);
            for (Ref<EntityStore> npc : List.copyOf(nearby)) {
                if (npc.isValid() && seen.add(npc)) visit(world, store, npc, playerRef, player.getWorldMapTracker());
            }
        }
    }

    private void visit(@Nonnull World world, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull WorldMapTracker tracker) {
        NPCEntity entity = store.getComponent(ref, NPCEntity.getComponentType());
        if (entity == null) return;
        service.edit(store, ref, npc -> {
            if (npc.identity().getCharacter() != null) {
                service.apply(npc);
                return null;
            }
            int drawsBefore = npc.identity().getDraws().size();
            String drawn = service.draw(npc, context(store, ref, player, world, entity, tracker));
            if (npc.identity().getDraws().size() != drawsBefore) LOGGER.atFine().log("NPC %s (%s) drew %s.", npc.uuid(), entity.getRoleName(), drawn == null ? "nobody" : "character '" + drawn + "'");
            return null;
        });
    }

    @Nonnull
    private static FilterContext context(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world, @Nonnull NPCEntity npc, @Nonnull WorldMapTracker tracker) {
        WorldMapTracker.ZoneDiscoveryInfo zone = tracker.getCurrentZone();
        int roleIndex = npc.getRoleIndex();
        return new FilterContext(store, ref, player, world.getName(), npc.getRoleName(), zone == null ? null : zone.regionName(), tracker.getCurrentBiomeName(), group -> {
            int index = NPCGroup.getAssetMap().getIndexOrDefault(group, -1);
            return index >= 0 && WorldSupport.hasTagInGroup(index, roleIndex);
        });
    }
}
