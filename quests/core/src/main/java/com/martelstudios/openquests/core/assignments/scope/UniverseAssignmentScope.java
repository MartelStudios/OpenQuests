package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.assignments.AssignmentTargets;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;

import javax.annotation.Nonnull;

/**
 * One quest shared by every player on the server, joined as they connect.
 */
public class UniverseAssignmentScope extends AssignmentScope {

    public static final String TYPE = "Universe";

    public static final BuilderCodec<UniverseAssignmentScope> CODEC = BuilderCodec.builder(UniverseAssignmentScope.class, UniverseAssignmentScope::new, AssignmentScope.BASE_CODEC).build();

    /**
     * The player still connecting, if this is their connection, joins a quest created now.
     */
    @Override
    public void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion, @Nonnull AssignmentTargets targets) {
        targets.universe(occasion.getPlayerId());
    }
}
