package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.martelstudios.opendialogues.characters.filters.FilterContext;

import javax.annotation.Nonnull;

/**
 * Describes an NPC to the character filters, from where it stands rather than where a player is.
 */
public final class FilterContextFactory {

    private FilterContextFactory() {}

    /**
     * @return what the filters read of the NPC; on its world thread.
     */
    @Nonnull
    public static FilterContext of(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull World world, @Nonnull NPCEntity npc) {
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
