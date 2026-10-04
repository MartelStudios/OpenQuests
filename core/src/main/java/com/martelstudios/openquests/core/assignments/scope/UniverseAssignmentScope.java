package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;

import javax.annotation.Nonnull;

/**
 * One quest shared by every player on the server, joined as they connect.
 */
public class UniverseAssignmentScope extends AssignmentScope {

    public static final String TYPE = "Universe";

    public static final BuilderCodec<UniverseAssignmentScope> CODEC = BuilderCodec.builder(UniverseAssignmentScope.class, UniverseAssignmentScope::new, AssignmentScope.BASE_CODEC).build();

    /**
     * The player still connecting, if this is their connection, joins a quest created now, not
     * being online yet for the server to hand it to them.
     */
    @Override
    public void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion) {
        QuestAssignmentService service = QuestAssignmentService.get();
        String key = timeKey(occasion);

        if (occasion.isTimed() && service.isSettled(UniverseQuestService.UNIVERSE_INDEX_KEY, assignment, key)) return;

        service.offerAll(assignment, service.universeHolder(occasion.getPlayerId()), key, occasion.isTimed());
    }
}
