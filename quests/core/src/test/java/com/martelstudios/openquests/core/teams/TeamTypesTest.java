package com.martelstudios.openquests.core.teams;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.martelstudios.openquests.core.teams.TestTeamTypes.type;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamTypesTest {

    @Test
    void aTypeIsFoundById() {
        TeamTypes types = TeamTypes.of(List.of(type("party", true)));

        assertTrue(types.get("party").isPersonalTeam());
        assertNull(types.get("faction"));
    }
}
