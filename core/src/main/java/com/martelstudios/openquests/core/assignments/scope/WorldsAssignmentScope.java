package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.assignments.trigger.AssignmentTrigger;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.utils.WorldNamePattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * One quest shared by every world the assignment gathers, the group named after the assignment:
 * the worlds the pattern names, or those its trigger happens in. Each world takes the group's
 * running quests up as it is entered, whatever the trigger.
 */
public class WorldsAssignmentScope extends AssignmentScope {

    public static final String TYPE = "Worlds";

    public static final BuilderCodec<WorldsAssignmentScope> CODEC = BuilderCodec.builder(WorldsAssignmentScope.class, WorldsAssignmentScope::new, AssignmentScope.BASE_CODEC)
                                                                                .append(new KeyedCodec<>("WorldNamePattern", Codec.STRING), (scope, pattern) -> scope.worldNamePattern = pattern == null ? null : WorldNamePattern.of(pattern), scope -> scope.worldNamePattern == null ? null : scope.worldNamePattern.getSource())
                                                                                .add()
                                                                                .build();

    @Nullable
    protected WorldNamePattern worldNamePattern;

    public WorldsAssignmentScope() {}

    public WorldsAssignmentScope(@Nullable String worldNamePattern) {
        this.worldNamePattern = worldNamePattern == null ? null : WorldNamePattern.of(worldNamePattern);
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
    public void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion) {
        QuestAssignmentService service = QuestAssignmentService.get();
        String key = timeKey(occasion);

        if (occasion.isTimed() && service.isSettled(WorldQuestService.groupKey(assignment.getId()), assignment, key)) return;

        World entered = occasion.getWorld() != null && gathers(assignment, occasion.getWorld()) ? occasion.getWorld() : null;
        service.offerAll(assignment, service.groupHolder(assignment.getId(), world -> gathers(assignment, world), entered, occasion.getPlayerId()), key, occasion.isTimed());
    }

    /**
     * Brings the group's running quests to a world of the group as someone enters it, and hands
     * them to that player, who is among its players only once in.
     */
    @Override
    public void onEnterWorld(@Nonnull OpenQuestAssignment assignment, @Nonnull UUID playerId, @Nonnull World world) {
        if (!gathers(assignment, world)) return;

        for (UUID questId : WorldQuestService.get().getGroupQuestIds(assignment.getId())) {
            // Read back first: after a restart, nothing else has brought the group's quests into memory
            AbstractQuestProgression<?> quest = QuestProgressionService.get().loadQuest(questId);
            if (quest == null || QuestProgressionService.get().getLiveQuest(questId) == null) continue;

            WorldQuestService.get().addQuest(world, questId);
            QuestProgressionService.get().joinQuest(quest, playerId);
        }
    }

    @Nullable
    @Override
    public String findInconsistency(@Nonnull AssignmentTrigger trigger) {
        if (worldNamePattern != null && worldNamePattern.getError() != null) return "WorldNamePattern does not compile: " + worldNamePattern.getError();
        if (worldNamePattern == null && !trigger.hasPlace()) return "the Worlds scope needs a WorldNamePattern under a trigger happening in no world";
        return null;
    }
}
