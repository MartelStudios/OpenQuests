package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.SqlDialect;
import com.martelstudios.openquests.core.persistence.disk.DiskQuestStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Both backends keep a team whole and refuse changes made against a superseded version.
 */
class TeamStoreTest {

    private QuestStorage storage;
    @TempDir Path folder;

    @AfterEach
    void close() {
        if (storage != null) storage.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "disk"})
    void aTeamSurvivesItsStore(String backend) {
        TeamStore teams = open(backend);
        UUID alice = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        TeamRecord team = team(alice).withMembers(List.of(alice, bob)).withInvitations(List.of(new TeamInvitation(UUID.randomUUID(), alice, 1234L)));

        assertTrue(teams.createTeam(team));
        TeamRecord read = teams.loadTeam(team.getId());

        assertEquals(team.getId(), read.getId());
        assertEquals("party", read.getType());
        assertEquals("Alice's team", read.getName());
        assertEquals("Alice", read.getNamedAfter());
        assertEquals(alice, read.getOwner());
        assertEquals(List.of(alice, bob), read.getMembers());
        assertEquals(team.getInvitations(), read.getInvitations());
        assertFalse(teams.createTeam(team));
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "disk"})
    void aStaleUpdateIsRefused(String backend) {
        TeamStore teams = open(backend);
        TeamRecord team = team(UUID.randomUUID());
        teams.createTeam(team);

        TeamRecord first = teams.loadTeam(team.getId());
        TeamRecord second = teams.loadTeam(team.getId());

        assertTrue(teams.updateTeam(first, first.withName("First")));
        assertFalse(teams.updateTeam(second, second.withName("Second")));
        assertEquals("First", teams.loadTeam(team.getId()).getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "disk"})
    void aDeletedTeamIsGone(String backend) {
        TeamStore teams = open(backend);
        TeamRecord team = team(UUID.randomUUID());
        teams.createTeam(team);

        assertTrue(teams.deleteTeam(team.getId(), 0));

        assertNull(teams.loadTeam(team.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "disk"})
    void aStaleDeleteIsRefused(String backend) {
        TeamStore teams = open(backend);
        TeamRecord team = team(UUID.randomUUID());
        teams.createTeam(team);
        TeamRecord read = teams.loadTeam(team.getId());
        teams.updateTeam(read, read.withName("Changed since"));

        assertFalse(teams.deleteTeam(team.getId(), 0));
        assertEquals("Changed since", teams.loadTeam(team.getId()).getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "disk"})
    void membersKeepTheirOrder(String backend) {
        TeamStore teams = open(backend);
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        TeamRecord team = team(c).withMembers(List.of(c, a, b));
        teams.createTeam(team);

        assertEquals(List.of(c, a, b), teams.loadTeam(team.getId()).getMembers());
    }

    @Nonnull
    private static TeamRecord team(@Nonnull UUID owner) {
        return new TeamRecord(UUID.randomUUID(), "party", "Alice's team", "Alice", owner, List.of(owner), List.of());
    }

    @Nonnull
    private TeamStore open(String backend) {
        String url = "jdbc:h2:mem:openquests-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        storage = backend.equals("disk") ? new DiskQuestStorage(folder)
                : new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(url, null, null, null, null, "oqtest_", 4, 10, true, SqlDialect.fromUrl(url)));
        storage.start();
        return storage;
    }
}
