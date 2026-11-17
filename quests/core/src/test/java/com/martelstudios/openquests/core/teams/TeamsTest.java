package com.martelstudios.openquests.core.teams;

import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.persistence.TeamStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.martelstudios.openquests.core.teams.TestTeamTypes.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamsTest {

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private final UUID carol = UUID.randomUUID();

    private QuestHarness harness;
    private Teams teams;

    @BeforeEach
    void open() {
        harness = new QuestHarness();
        harness.teamTypes().add(type("party", true));
        teams = harness.services().getTeams();
    }

    @AfterEach
    void close() {
        harness.close();
    }

    @Test
    void aNewTeamIsOwnedByItsCreator() {
        TeamRecord team = teams.create("party", alice, "Alice's team");

        TeamRecord read = teams.get(team.getId());
        assertEquals(alice, read.getOwner());
        assertEquals(List.of(alice), read.getMembers());
        assertEquals("party", read.getType());
        assertEquals("Alice's team", read.getName());
    }

    @Test
    void aMemberIsAppended() {
        TeamRecord team = teams.create("party", alice, "Alice's team");

        teams.addMember(team.getId(), bob);

        assertEquals(List.of(alice, bob), teams.get(team.getId()).getMembers());
    }

    @Test
    void theFirstMemberSucceedsTheOwner() {
        UUID teamId = teams.create("party", alice, "Alice's team").getId();
        teams.addMember(teamId, bob);
        teams.addMember(teamId, carol);

        TeamRecord left = teams.removeMember(teamId, alice);

        assertEquals(bob, left.getOwner());
        assertEquals(List.of(bob, carol), left.getMembers());
        assertEquals(left.getMembers(), teams.get(teamId).getMembers());
    }

    @Test
    void theLastMemberLeavingDeletesTheTeam() {
        UUID teamId = teams.create("party", alice, "Alice's team").getId();

        assertNull(teams.removeMember(teamId, alice));
        assertNull(teams.get(teamId));
    }

    @Test
    void onlyTheOwnerInvitesAndRenames() {
        UUID teamId = teams.create("party", alice, "Alice's team").getId();
        teams.addMember(teamId, bob);

        assertThrows(IllegalArgumentException.class, () -> teams.invite(teamId, bob, carol));
        assertThrows(IllegalArgumentException.class, () -> teams.rename(teamId, bob, "Bob's"));

        teams.invite(teamId, alice, carol);
        teams.rename(teamId, alice, "The Three");
        assertEquals("The Three", teams.get(teamId).getName());
        assertEquals(List.of(teamId), teams.invitationsOf(carol, "party").stream().map(TeamRecord::getId).toList());
    }

    @Test
    void anInvitationExpires() {
        UUID teamId = teams.create("party", alice, "Alice's team").getId();
        teams.invite(teamId, alice, bob);

        harness.clock().advance(Duration.ofHours(25));

        assertTrue(teams.invitationsOf(bob, "party").isEmpty());
    }

    @Test
    void aLostWriteIsRetried() {
        AtomicBoolean lost = new AtomicBoolean();
        TeamStore store = harness.storage();
        TeamStore losingOnce = new TeamStore() {
            @Nullable
            @Override
            public TeamRecord loadTeam(@Nonnull UUID teamId) {
                return store.loadTeam(teamId);
            }

            @Override
            public boolean createTeam(@Nonnull TeamRecord team) {
                return store.createTeam(team);
            }

            /**
             * Another server's rename lands first, as if it had written between the read and this write.
             */
            @Override
            public boolean updateTeam(@Nonnull TeamRecord expected, @Nonnull TeamRecord next) {
                if (lost.compareAndSet(false, true)) {
                    store.updateTeam(expected, expected.withName("Renamed elsewhere"));
                    return false;
                }
                return store.updateTeam(expected, next);
            }

            @Override
            public void deleteTeam(@Nonnull UUID teamId) {
                store.deleteTeam(teamId);
            }
        };
        Teams retrying = new Teams(losingOnce, harness.storage(), TeamTypes.of(harness.teamTypes()), harness.clock(), Duration.ofHours(24));
        UUID teamId = retrying.create("party", alice, "Alice's team").getId();

        retrying.addMember(teamId, bob);

        TeamRecord read = teams.get(teamId);
        assertEquals(List.of(alice, bob), read.getMembers());
        assertEquals("Renamed elsewhere", read.getName());
    }
}
