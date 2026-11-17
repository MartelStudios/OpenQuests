package com.martelstudios.openquests.core.assignments;

import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.teams.TestTeamTypes;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamAssignmentTest {

    @Test
    void twoMembersShareOneQuest() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            TeamQuestService teams = harness.services().getTeamQuestService();
            UUID alice = harness.players().connect();
            UUID bob = harness.players().connect();
            UUID amis = harness.services().getTeams().create("party", alice, "Alice", "Amis").getId();
            harness.services().getTeams().addMember(amis, bob);

            AssignmentHolders holders = new AssignmentHolders(new SharedAssignmentRecords(harness.storage()), harness.services().getQuestProgressionService(), harness.services().getScopeServices(), harness.players().types());
            AssignmentRecord next = AssignmentRecord.next(null, "once", Instant.now());

            // Straight to the holders: a quest built from an asset id is set aside without an asset store
            assertTrue(holders.team(amis, alice).handOut(new TestQuestProgression(), "Story", "Test", null, next));
            AssignmentHolder forBob = holders.team(amis, bob);
            assertEquals(next, forBob.getRecord("Story", "Test"));
            assertFalse(forBob.handOut(new TestQuestProgression(), "Story", "Test", null, next));

            assertEquals(1, teams.getQuestIds(amis).size());
            AbstractQuestProgression<?> quest = harness.services().getQuestProgressionService().getQuest(teams.getQuestIds(amis).iterator().next());
            assertEquals(Set.of(alice, bob), Set.copyOf(quest.getPlayers()));
        }
    }

    @Test
    void aSoloPlayerIsReachedThroughTheirPersonalTeam() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            UUID alice = host(harness, harness.players().connect());
            harness.services().getTeamService().onConnect(alice, "Alice", harness.players().of(alice));

            Set<UUID> reached = harness.services().getQuestAssignmentService().teamsReached("party", Occasion.connection(alice, harness.players().of(alice)));

            assertEquals(Set.of(questStore(harness, alice).getTeams().get("party")), reached);
        }
    }

    @Test
    void aPlayerWithoutAFactionGetsNothing() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            UUID alice = host(harness, harness.players().connect());

            assertTrue(harness.services().getQuestAssignmentService().teamsReached("faction", Occasion.connection(alice, harness.players().of(alice))).isEmpty());
        }
    }

    @Test
    void aScheduleOffersEachTeamOnce() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            UUID alice = host(harness, harness.players().connect());
            UUID bob = host(harness, harness.players().connect());
            UUID carol = host(harness, harness.players().connect());
            UUID lion = UUID.randomUUID();
            questStore(harness, alice).setTeam("faction", lion);
            questStore(harness, bob).setTeam("faction", lion);
            questStore(harness, carol).setTeam("faction", UUID.randomUUID());
            harness.players().disconnect(carol);

            Set<UUID> reached = harness.services().getQuestAssignmentService().teamsReached("faction", Occasion.period("period:2026-11-16T00:00:00Z"));

            assertEquals(Set.of(lion), reached);
        }
    }

    /**
     * Opens the player's session as their connection would, which a schedule reads teams from.
     */
    @Nonnull
    private static UUID host(@Nonnull QuestHarness harness, @Nonnull UUID playerId) {
        harness.services().getQuestPlayerStateService().host(playerId, harness.players().of(playerId));
        return playerId;
    }

    @Nonnull
    private static QuestStoreComponent questStore(@Nonnull QuestHarness harness, @Nonnull UUID playerId) {
        return harness.players().of(playerId).ensureAndGetComponent(harness.players().types().questStore());
    }
}
