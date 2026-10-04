package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.assignments.AssignmentTargets;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One quest shared by every world the assignment gathers, the group named after the assignment:
 * the worlds the pattern names, or those its trigger happens in. Each world takes the group's
 * running quests up as it is entered, whatever the trigger.
 */
public class WorldsAssignmentScope extends AbstractWorldAssignmentScope {

    public static final String TYPE = "Worlds";

    public static final BuilderCodec<WorldsAssignmentScope> CODEC = BuilderCodec.builder(WorldsAssignmentScope.class, WorldsAssignmentScope::new, AbstractWorldAssignmentScope.BASE_CODEC).build();

    public WorldsAssignmentScope() {}

    public WorldsAssignmentScope(@Nullable String worldNamePattern) {
        super(worldNamePattern);
    }

    /**
     * @return whether that world belongs to the group the assignment gathers.
     */
    public boolean gathers(@Nonnull OpenQuestAssignment assignment, @Nonnull World world) {
        return worldNamePattern != null ? worldNamePattern.matches(world.getName()) : assignment.getTrigger().happensIn(world);
    }

    /**
     * The group is one holder wherever the occasion happens, so only its time tells hand-outs
     * apart: entering a second world of the group joins the quest the first one started.
     */
    @Override
    public void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion, @Nonnull AssignmentTargets targets) {
        boolean entersGroup = occasion.getWorld() != null && gathers(assignment, occasion.getWorld());
        targets.group(assignment.getId(), entersGroup ? occasion.getPlayerId() : null);
    }

    /**
     * The group is named after the assignment gathering it.
     */
    @Nullable
    @Override
    public String groupOf(@Nonnull OpenQuestAssignment assignment, @Nonnull World world) {
        return gathers(assignment, world) ? assignment.getId() : null;
    }
}
