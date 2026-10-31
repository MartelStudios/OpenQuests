package com.martelstudios.opendialogues.characters.filters;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * An NPC as the scan found it near a player.
 *
 * @param zone    the region the player stands in, {@code null} before the map knows it.
 * @param biome   the biome the player stands in, {@code null} before the map knows it.
 * @param inGroup whether the NPC's role belongs to an {@code NPCGroup}, by group id.
 */
public record FilterContext(@Nonnull String world, @Nonnull String role, @Nullable String zone, @Nullable String biome, @Nonnull Predicate<String> inGroup) {}
