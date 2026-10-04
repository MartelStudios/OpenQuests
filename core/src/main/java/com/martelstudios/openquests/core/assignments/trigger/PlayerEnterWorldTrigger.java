package com.martelstudios.openquests.core.assignments.trigger;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.utils.WorldNamePattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * As a player enters a world whose whole name matches the pattern, connecting into it included.
 * Without a pattern, every world counts.
 */
public class PlayerEnterWorldTrigger extends AssignmentTrigger {

    public static final String TYPE = "PlayerEnterWorld";

    public static final BuilderCodec<PlayerEnterWorldTrigger> CODEC = BuilderCodec.builder(PlayerEnterWorldTrigger.class, PlayerEnterWorldTrigger::new, AssignmentTrigger.BASE_CODEC)
                                                                                  .append(new KeyedCodec<>("WorldNamePattern", WorldNamePattern.CODEC), (trigger, pattern) -> trigger.worldNamePattern = pattern, trigger -> trigger.worldNamePattern)
                                                                                  .add()
                                                                                  .build();

    @Nullable
    protected WorldNamePattern worldNamePattern;

    public PlayerEnterWorldTrigger() {}

    public PlayerEnterWorldTrigger(@Nullable String worldNamePattern) {
        this.worldNamePattern = WorldNamePattern.ofNullable(worldNamePattern);
    }

    /**
     * @return whether entering a world of that name counts.
     */
    public boolean matches(@Nonnull String worldName) {
        return worldNamePattern == null || worldNamePattern.matches(worldName);
    }

    @Nullable
    @Override
    public Occasion onEnterWorld(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull World world) {
        return matches(world.getName()) ? Occasion.entry(playerId, player, world) : null;
    }

    @Override
    public boolean happensIn(@Nonnull World world) {
        return matches(world.getName());
    }

    @Override
    public boolean hasPlace() {
        return true;
    }

    @Nullable
    @Override
    public String findInconsistency() {
        return WorldNamePattern.findError(worldNamePattern);
    }
}
