package com.martelstudios.opendialogues.characters.filters;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.PlayerRef;
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
 * @param player      the player whose scan found the NPC, {@code null} for a draw asked by a mod.
 * @param playerZone  the region the player stands in, {@code null} before the map knows it.
 * @param playerBiome the biome the player stands in, {@code null} before the map knows it.
 * @param inGroup     whether the NPC's role belongs to an {@code NPCGroup}, by group id.
 */
public record FilterContext(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> npc, @Nullable PlayerRef player, @Nonnull String world, @Nonnull String role, @Nullable String playerZone, @Nullable String playerBiome, @Nonnull Predicate<String> inGroup) {}
