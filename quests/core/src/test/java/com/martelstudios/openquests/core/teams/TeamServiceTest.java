package com.martelstudios.openquests.core.teams;

import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.TestQuestAsset;
import com.martelstudios.openquests.core.constraints.AssignmentRequest;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.events.TeamJoinedEvent;
import com.martelstudios.openquests.core.events.TeamLeftEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamServiceTest {

    private QuestHarness harness;
    private Teams teams;
    private TeamQuestService teamQuests;
    private TeamService service;
    private UUID alice;
    private UUID bob;

    @BeforeEach
    void open() {
        harness = new QuestHarness();
        harness.teamTypes().add(TestTeamTypes.type("party", true));
        harness.teamTypes().add(TestTeamTypes.type("faction", false));
        teams = harness.services().getTeams();
        teamQuests = harness.services().getTeamQuestService();
        service = harness.services().getTeamService();
        alice = connect("Alice");
        bob = connect("Bob");
    }

    @AfterEach
    void close() {
        harness.close();
    }

    @Test
    void joiningDropsTheProgressOfATeamLeftEmpty() {
        UUID own = party(alice);
        TestQuestProgression lost = share(own, harness.register());
        harness.storage().claimAssignment(TeamQuestService.key(own), "Story", "Test", null, record());

        service.join(alice, harness.players().of(alice), party(bob));

        assertNull(teams.get(own));
        assertTrue(harness.storage().loadAssignments(TeamQuestService.key(own)).isEmpty());
        assertNull(harness.services().getQuestProgressionService().getQuest(lost.getId()));
        assertFalse(harness.questsOf(alice).contains(lost.getId()));
    }

    @Test
    void aNewcomerJoinsDespiteTheConstraints() {
        UUID bobs = party(bob);
        TestQuestProgression quest = share(bobs, harness.register(new TestQuestAsset(new Refusing())));
        harness.hear(TeamJoinedEvent.class);

        service.join(alice, harness.players().of(alice), bobs);

        assertTrue(quest.getPlayers().contains(alice));
        assertEquals(bobs, questStore(alice).getTeams().get("party"));
        assertEquals(List.of(bob, alice), teams.get(bobs).getMembers());
        assertTrue(harness.heard().contains(TeamJoinedEvent.class));
    }

    @Test
    void aNewcomerIsNotPaidForThePast() {
        UUID bobs = party(bob);
        TestQuestProgression ended = share(bobs, harness.register());
        harness.count(ended, 5, 5);
        harness.settle();

        service.join(alice, harness.players().of(alice), bobs);

        assertTrue(ended.isOver());
        assertFalse(ended.getPlayers().contains(alice));
    }

    @Test
    void leavingAPartyKeepsACopy() {
        UUID bobs = party(bob);
        service.join(alice, harness.players().of(alice), bobs);
        TestQuestProgression quest = share(bobs, harness.register());
        harness.count(quest, 3, 5);
        harness.hear(TeamLeftEvent.class);

        service.leave(alice, "Alice", harness.players().of(alice), "party");

        UUID own = questStore(alice).getTeams().get("party");
        assertNotEquals(bobs, own);
        assertEquals(alice, teams.get(own).getOwner());
        TestQuestProgression copy = onlyQuestOf(own);
        assertEquals(3, copy.getCounter());
        assertEquals(Set.of(alice), Set.copyOf(copy.getPlayers()));
        assertFalse(quest.getPlayers().contains(alice));
        assertTrue(harness.heard().contains(TeamLeftEvent.class));
    }

    @Test
    void leavingAPartyAloneChangesNothing() {
        UUID own = party(alice);
        TestQuestProgression quest = share(own, harness.register());

        service.leave(alice, "Alice", harness.players().of(alice), "party");
        service.change(alice, "Alice", "party", null);

        assertEquals(own, party(alice));
        assertEquals(Set.of(quest.getId()), teamQuests.getQuestIds(own));
        assertTrue(quest.getPlayers().contains(alice));
    }

    @Test
    void anAbsentPlayerLeavingAPartyAloneChangesNothing() {
        UUID carol = UUID.randomUUID();
        UUID carols = teams.create("party", carol, "Carol", null).getId();
        PlayerQuestRecord record = new PlayerQuestRecord();
        record.getTeams().put("party", carols);
        harness.storage().savePlayer(carol, record, List.of(), true);

        service.change(carol, "Carol", "party", null);

        assertEquals(List.of(carol), teams.get(carols).getMembers());
        assertTrue(harness.storage().loadMessages(List.of(carol)).isEmpty());
    }

    @Test
    void leavingKeepsTheTeamForTheOthers() {
        UUID own = party(alice);
        service.join(bob, harness.players().of(bob), own);
        TestQuestProgression quest = share(own, harness.register());

        service.leave(alice, "Alice", harness.players().of(alice), "party");

        TeamRecord left = teams.get(own);
        assertEquals(bob, left.getOwner());
        assertEquals(List.of(bob), left.getMembers());
        assertFalse(quest.isOver());
        assertEquals(Set.of(bob), Set.copyOf(quest.getPlayers()));
    }

    @Test
    void aLeaverIsNotHandedTheLineAgain() {
        UUID bobs = party(bob);
        service.join(alice, harness.players().of(alice), bobs);
        AssignmentRecord handed = record();
        harness.storage().claimAssignment(TeamQuestService.key(bobs), "Story", "Test", null, handed);

        service.leave(alice, "Alice", harness.players().of(alice), "party");

        UUID own = questStore(alice).getTeams().get("party");
        assertEquals(handed, harness.storage().loadAssignments(TeamQuestService.key(own)).get("Story", "Test"));
    }

    @Test
    void leavingAFactionCopiesNothing() {
        UUID lions = teams.create("faction", bob, "Bob", "Lions").getId();
        questStore(bob).setTeam("faction", lions);
        service.join(alice, harness.players().of(alice), lions);
        share(lions, harness.register());

        service.leave(alice, "Alice", harness.players().of(alice), "faction");

        assertNull(questStore(alice).getTeams().get("faction"));
        assertTrue(harness.questsOf(alice).isEmpty());
        assertEquals(List.of(bob), teams.get(lions).getMembers());
    }

    @Test
    void wouldLoseNamesTheTeam() {
        UUID own = party(alice);
        assertNull(service.wouldLose(alice, harness.players().of(alice), "party"));

        share(own, harness.register());
        assertEquals(own, service.wouldLose(alice, harness.players().of(alice), "party").getId());

        teams.addMember(own, bob);
        assertNull(service.wouldLose(alice, harness.players().of(alice), "party"));
    }

    @Test
    void joiningATeamThatEmptiedKeepsTheOldOne() {
        UUID own = party(alice);
        TestQuestProgression quest = share(own, harness.register());
        UUID bobs = party(bob);
        TeamRecord read = teams.get(bobs);
        harness.storage().updateTeam(read, read.withMembers(List.of()));

        assertThrows(IllegalArgumentException.class, () -> service.join(alice, harness.players().of(alice), bobs));

        assertEquals(own, questStore(alice).getTeams().get("party"));
        assertEquals(List.of(alice), teams.get(own).getMembers());
        assertTrue(quest.getPlayers().contains(alice));
    }

    @Test
    void anOnlineChangeThatFailsLeavesThePlayerWhereTheyWere() {
        UUID own = party(alice);
        UUID bobs = party(bob);
        TeamRecord read = teams.get(bobs);
        harness.storage().updateTeam(read, read.withMembers(List.of()));

        assertDoesNotThrow(() -> service.change(alice, "Alice", "party", bobs));

        assertEquals(own, questStore(alice).getTeams().get("party"));
    }

    @Test
    void aChangeFromAStaleRecordLeavesTheTeamThePlayerIsReallyIn() {
        UUID bobs = party(bob);
        service.join(alice, harness.players().of(alice), bobs);
        UUID lions = teams.create("party", UUID.randomUUID(), "Carol", "Lions").getId();
        teams.addMember(lions, alice);
        UUID stale = UUID.randomUUID();

        // Written by a server that read alice's record before she joined bob
        service.apply(alice, harness.players().of(alice), new TeamChange("party", stale, lions));

        assertEquals(lions, questStore(alice).getTeams().get("party"));
        assertFalse(teams.get(bobs).getMembers().contains(alice));
    }

    @Test
    void aStaleChangeIsIgnored() {
        UUID own = party(alice);
        UUID bobs = party(bob);
        share(bobs, harness.register());

        service.apply(alice, harness.players().of(alice), new TeamChange("party", own, bobs));

        assertEquals(own, questStore(alice).getTeams().get("party"));
        assertTrue(harness.questsOf(alice).isEmpty());
    }

    @Nonnull
    private UUID connect(@Nonnull String name) {
        UUID playerId = harness.players().connect();
        service.onConnect(playerId, name, harness.players().of(playerId));
        return playerId;
    }

    @Nonnull
    private UUID party(@Nonnull UUID playerId) {
        return questStore(playerId).getTeams().get("party");
    }

    @Nonnull
    private TestQuestProgression share(@Nonnull UUID teamId, @Nonnull TestQuestProgression quest) {
        new TeamQuestScope(teamId).share(harness.services().getScopeServices(), quest);
        return quest;
    }

    @Nonnull
    private TestQuestProgression onlyQuestOf(@Nonnull UUID teamId) {
        Set<UUID> ids = teamQuests.getQuestIds(teamId);
        assertEquals(1, ids.size());
        AbstractQuestProgression<?> quest = harness.services().getQuestProgressionService().getQuest(ids.iterator().next());
        return (TestQuestProgression) quest;
    }

    @Nonnull
    private QuestStoreComponent questStore(@Nonnull UUID playerId) {
        return harness.players().of(playerId).ensureAndGetComponent(harness.players().types().questStore());
    }

    @Nonnull
    private static AssignmentRecord record() {
        return AssignmentRecord.next(null, "once", Instant.ofEpochMilli(1000));
    }

    /**
     * Turns every player away.
     */
    private static final class Refusing extends QuestConstraint {
        @Override
        public boolean allowsAssignment(@Nonnull AssignmentRequest request) {
            return false;
        }
    }
}
