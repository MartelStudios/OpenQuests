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
import com.martelstudios.opendialogues.map.CharacterPositions;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Every few seconds, finds the NPCs near each player of a world: those carrying a character get
 * their traits applied again, the others may draw one. The character service does the rest.
 */
public class CharacterScanSystem extends DelayedSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Supplier<CharacterSettings> settings;
    private final double radius;
    private final CharacterService service;
    private final Function<Store<EntityStore>, CharacterPositions> positions;

    /**
     * @param settings  read anew on each visit, so stopping draws needs no restart.
     * @param positions a world's character positions, where each visit records its NPC.
     */
    public CharacterScanSystem(@Nonnull Supplier<CharacterSettings> settings, @Nonnull CharacterService service, @Nonnull Function<Store<EntityStore>, CharacterPositions> positions) {
        super(settings.get().getScanInterval());
        this.settings = settings;
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
            } else if (settings.get().isDraws()) {
                int drawsBefore = npc.identity().getDraws().size();
                String drawn = service.draw(npc, context(store, ref, player, world, entity, tracker));
                if (npc.identity().getDraws().size() != drawsBefore) LOGGER.atFine().log("NPC %s (%s) drew %s.", npc.uuid(), entity.getRoleName(), drawn == null ? "nobody" : "character '" + drawn + "'");
            }
            record(store, ref, npc);
            return null;
        });
    }

    /**
     * The visit's own position, so a marker follows its NPC at the scan's pace at most.
     */
    private void record(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull CharacterNpc npc) {
        String characterId = npc.identity().getCharacter();
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (characterId == null || transform == null) return;
        positions.apply(store).record(characterId, npc.uuid(), transform.getPosition(), npc.identity().getName());
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
