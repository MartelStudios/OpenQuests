package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

/**
 * Gives an NPC its character as it enters the world, spawned or loaded with its chunk, so players
 * never see the draw: one without a character may draw one, one carrying a character gets its traits
 * applied again.
 */
public class CharacterSpawnSystem extends RefSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Supplier<CharacterSettings> settings;
    private final CharacterService service;

    /**
     * @param settings read anew on each NPC, so stopping draws needs no restart.
     */
    public CharacterSpawnSystem(@Nonnull Supplier<CharacterSettings> settings, @Nonnull CharacterService service) {
        this.settings = settings;
        this.service = service;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    /**
     * Deferred past the tick, during which the store refuses the components traits add.
     */
    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        World world = store.getExternalData().getWorld();
        world.execute(() -> {
            if (ref.isValid()) visit(world, store, ref);
        });
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {}

    private void visit(@Nonnull World world, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        NPCEntity entity = store.getComponent(ref, NPCEntity.getComponentType());
        if (entity == null) return;
        service.edit(store, ref, npc -> {
            if (npc.identity().getCharacter() != null) {
                service.apply(npc);
            } else if (settings.get().isDraws()) {
                int drawsBefore = npc.identity().getDraws().size();
                String drawn = service.draw(npc, context(store, ref, world, entity));
                if (npc.identity().getDraws().size() != drawsBefore) LOGGER.atFine().log("NPC %s (%s) drew %s.", npc.uuid(), entity.getRoleName(), drawn == null ? "nobody" : "character '" + drawn + "'");
            }
            return null;
        });
    }

    @Nonnull
    private static FilterContext context(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull World world, @Nonnull NPCEntity npc) {
        ZoneBiomeResult place = place(world, store.getComponent(ref, TransformComponent.getComponentType()));
        String zone = place == null || place.getZoneResult() == null ? null : place.getZoneResult().getZone().name();
        String biome = place == null || place.getBiome() == null ? null : place.getBiome().getName();
        int roleIndex = npc.getRoleIndex();
        return new FilterContext(store, ref, world.getName(), npc.getRoleName(), zone, biome, group -> {
            int index = NPCGroup.getAssetMap().getIndexOrDefault(group, -1);
            return index >= 0 && WorldSupport.hasTagInGroup(index, roleIndex);
        });
    }

    /**
     * Read from the world generator as vanilla's {@code BiomeDataSystem} does for players; a world
     * without one (flat, void) has no zone nor biome.
     */
    private static ZoneBiomeResult place(@Nonnull World world, TransformComponent transform) {
        if (transform == null || !(world.getChunkStore().getGenerator() instanceof ChunkGenerator generator)) return null;
        return generator.getZoneBiomeResultAt((int) world.getWorldConfig().getSeed(), (int) transform.getPosition().x, (int) transform.getPosition().z);
    }
}
