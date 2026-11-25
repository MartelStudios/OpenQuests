package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.assignments.AssignmentTargets;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;
import com.martelstudios.openquests.core.teams.OpenQuestTeamType;

import javax.annotation.Nonnull;

/**
 * One quest per team of a type, shared by its members. The occasion's player stands for their
 * team; a schedule reaches every team with a member online, so a team nobody plays piles nothing up.
 */
public class TeamAssignmentScope extends AssignmentScope {

    public static final String TYPE = "Team";

    public static final BuilderCodec<TeamAssignmentScope> CODEC = BuilderCodec.builder(TeamAssignmentScope.class, TeamAssignmentScope::new, AssignmentScope.BASE_CODEC)
                                                                              .append(new KeyedCodec<>("TeamType", Codec.STRING), (scope, type) -> scope.teamType = type, scope -> scope.teamType)
                                                                              .addValidator(Validators.nonNull())
                                                                              .addValidator(OpenQuestTeamType.VALIDATOR_CACHE.getValidator())
                                                                              .documentation("Asset ID of the team type eligible for the assignment.")
                                                                              .add()
                                                                              .documentation("Shares one quest instance among the members of each eligible team.")
                                                                              .build();

    protected String teamType;

    protected TeamAssignmentScope() {}

    public TeamAssignmentScope(@Nonnull String teamType) {
        this.teamType = teamType;
    }

    /**
     * A player with no team of the type, only possible for a type without personal teams, gets nothing.
     */
    @Override
    public void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion, @Nonnull AssignmentTargets targets) {
        targets.team(teamType, occasion);
    }
}
