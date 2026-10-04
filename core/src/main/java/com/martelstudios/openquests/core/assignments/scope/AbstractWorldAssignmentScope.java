package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.assignments.trigger.AssignmentTrigger;
import com.martelstudios.openquests.core.utils.WorldNamePattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A scope sharing quests in worlds, named by its own pattern or, without one, by where its trigger
 * happens.
 */
public abstract class AbstractWorldAssignmentScope extends AssignmentScope {

    /**
     * Serializes the pattern every world scope may name its worlds by; concrete codecs chain from
     * this.
     */
    public static final BuilderCodec<AbstractWorldAssignmentScope> BASE_CODEC = BuilderCodec.abstractBuilder(AbstractWorldAssignmentScope.class, AssignmentScope.BASE_CODEC)
                                                                                            .append(new KeyedCodec<>("WorldNamePattern", WorldNamePattern.CODEC), (scope, pattern) -> scope.worldNamePattern = pattern, scope -> scope.worldNamePattern)
                                                                                            .add()
                                                                                            .build();

    @Nullable
    protected WorldNamePattern worldNamePattern;

    protected AbstractWorldAssignmentScope() {}

    protected AbstractWorldAssignmentScope(@Nullable String worldNamePattern) {
        this.worldNamePattern = WorldNamePattern.ofNullable(worldNamePattern);
    }

    @Nullable
    @Override
    public String findInconsistency(@Nonnull AssignmentTrigger trigger) {
        String error = WorldNamePattern.findError(worldNamePattern);
        if (error != null) return error;
        if (worldNamePattern == null && !trigger.hasPlace()) return "a scope sharing quests in worlds needs a WorldNamePattern under a trigger happening in no world";
        return null;
    }
}
