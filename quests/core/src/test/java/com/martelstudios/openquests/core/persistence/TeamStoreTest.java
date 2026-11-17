package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.SqlDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shared backend keeps a team whole, and refuses a change made on a version another server
 * replaced. The disk one needs a running universe to place its files, so no test reaches it.
 */
class TeamStoreTest {

    private QuestStorage storage;

    @AfterEach
    void close() {
        if (storage != null) storage.close();
    }

    @Test
    void aTeamSurvivesItsStore() {
        TeamStore teams = open();
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        TeamRecord team = team(alice).withMembers(List.of(alice, bob)).withInvitations(List.of(new TeamInvitation(UUID.randomUUID(), alice, 1234L)));

        assertTrue(teams.createTeam(team));
        TeamRecord read = teams.loadTeam(team.getId());

        assertEquals(team.getId(), read.getId());
        assertEquals("party", read.getType());
        assertEquals("Alice's team", read.getName());
        assertEquals(alice, read.getOwner());
        assertEquals(List.of(alice, bob), read.getMembers());
        assertEquals(team.getInvitations(), read.getInvitations());
        assertFalse(teams.createTeam(team));
    }

    @Test
    void aStaleUpdateIsRefused() {
        TeamStore teams = open();
        TeamRecord team = team(UUID.randomUUID());
        teams.createTeam(team);

        TeamRecord first = teams.loadTeam(team.getId());
        TeamRecord second = teams.loadTeam(team.getId());

        assertTrue(teams.updateTeam(first, first.withName("First")));
        assertFalse(teams.updateTeam(second, second.withName("Second")));
        assertEquals("First", teams.loadTeam(team.getId()).getName());
    }

    @Test
    void aDeletedTeamIsGone() {
        TeamStore teams = open();
        TeamRecord team = team(UUID.randomUUID());
        teams.createTeam(team);

        teams.deleteTeam(team.getId());

        assertNull(teams.loadTeam(team.getId()));
    }

    @Test
    void membersKeepTheirOrder() {
        TeamStore teams = open();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        TeamRecord team = team(c).withMembers(List.of(c, a, b));
        teams.createTeam(team);

        assertEquals(List.of(c, a, b), teams.loadTeam(team.getId()).getMembers());
    }

    @Nonnull
    private static TeamRecord team(@Nonnull UUID owner) {
        return new TeamRecord(UUID.randomUUID(), "party", "Alice's team", owner, List.of(owner), List.of());
    }

    @Nonnull
    private TeamStore open() {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        storage = new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(url, null, null, null, null, "oqtest_", 4, 10, true, SqlDialect.fromUrl(url)));
        storage.start();
        return storage;
    }
}
