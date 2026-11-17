package com.martelstudios.openquests.core.teams;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamKeyTest {

    @Test
    void aTeamIsKeyedByTypeAndName() {
        TeamKey team = new TeamKey("party", "amis");

        assertEquals("team:party:amis", team.holderKey());
        assertEquals("team-members:party:amis", team.membersKey());
        assertFalse(team.isPersonal());
    }

    @Test
    void aPersonalTeamIsNamedAfterItsOwner() {
        UUID id = UUID.randomUUID();
        TeamKey team = TeamKey.personal("party", id);

        assertEquals("~" + id, team.name());
        assertTrue(team.isPersonal());
    }

    @Test
    void aHolderKeyReadsBackToItsTeam() {
        assertEquals(new TeamKey("party", "amis"), TeamKey.parseHolderKey("team:party:amis"));
        assertNull(TeamKey.parseHolderKey("universe"));
    }

    @Test
    void aNameThatWouldBreakTheKeyIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TeamKey("party", "a:b"));
        assertThrows(IllegalArgumentException.class, () -> new TeamKey("party", "~amis"));
    }
}
