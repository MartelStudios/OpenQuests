package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.assignments.AssignmentTargets;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;

import javax.annotation.Nonnull;

/**
 * A quest of their own for the player the occasion concerns, theirs to keep wherever they go. An
 * occasion concerning nobody in particular, a schedule, concerns every player online.
 */
public class PlayerAssignmentScope extends AssignmentScope {

    public static final String TYPE = "Player";

    public static final BuilderCodec<PlayerAssignmentScope> CODEC = BuilderCodec.builder(PlayerAssignmentScope.class, PlayerAssignmentScope::new, AssignmentScope.BASE_CODEC).build();

    @Override
    public void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion, @Nonnull AssignmentTargets targets) {
        if (occasion.getPlayerId() != null && occasion.getPlayer() != null) {
            targets.player(occasion.getPlayerId(), occasion.getPlayer());
        } else {
            targets.onlinePlayers();
        }
    }
}
