package com.martelstudios.openquests.core.teams;

import javax.annotation.Nonnull;

/**
 * Team types built in memory, where the server would read them from the packs.
 */
public final class TestTeamTypes {

    private TestTeamTypes() {}

    /**
     * @return a team type under that id, personal-team or not.
     */
    @Nonnull
    public static OpenQuestTeamType type(@Nonnull String id, boolean personalTeam) {
        OpenQuestTeamType type = new OpenQuestTeamType();
        type.id = id;
        type.nameKey = "openquests.team_type." + id;
        type.personalTeam = personalTeam;
        return type;
    }
}
