package com.martelstudios.opendialogues.characters.filters;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * An NPC about to draw a character. A filter needing more than the shortcuts reads the NPC through
 * the accessor, on the world thread.
 *
 * @param accessor    the NPC's store, for filters reading its components.
 * @param npc         the NPC entity.
 * @param zone        the region the NPC stands in, {@code null} in a world without generated zones.
 * @param biome       the biome the NPC stands in, {@code null} in a world without generated biomes.
 * @param inGroup     whether the NPC's role belongs to an {@code NPCGroup}, by group id.
 */
public record FilterContext(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> npc, @Nonnull String world, @Nonnull String role, @Nullable String zone, @Nullable String biome, @Nonnull Predicate<String> inGroup) {}
