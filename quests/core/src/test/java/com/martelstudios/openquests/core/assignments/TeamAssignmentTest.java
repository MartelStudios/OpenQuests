package com.martelstudios.openquests.core.assignments;

import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.assignments.repeat.OnceRepeat;
import com.martelstudios.openquests.core.assignments.scope.TeamAssignmentScope;
import com.martelstudios.openquests.core.assignments.trigger.PlayerConnectTrigger;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.teams.TeamKey;
import com.martelstudios.openquests.core.teams.TestTeamTypes;
import com.martelstudios.openquests.core.utils.EntityComponents;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamAssignmentTest {

    private static final TeamKey AMIS = new TeamKey("party", "amis");

    @Test
    void twoMembersShareOneQuest() {
        try (QuestHarness harness = new QuestHarness()) {
            TeamQuestService teams = harness.services().getTeamQuestService();
            UUID alice = harness.players().connect();
            UUID bob = harness.players().connect();
            teams.addMember(AMIS, alice);
            teams.addMember(AMIS, bob);

            AssignmentHolders holders = new AssignmentHolders(new SharedAssignmentRecords(harness.storage()), harness.services().getQuestProgressionService(), harness.services().getScopeServices(), harness.players().types());
            AssignmentRecord next = AssignmentRecord.next(null, "once", Instant.now());

            // Straight to the holders: a quest built from an asset id is set aside without an asset store
            assertTrue(holders.team(AMIS, alice).handOut(new TestQuestProgression(), "Story", "Test", null, next));
            AssignmentHolder forBob = holders.team(AMIS, bob);
            assertEquals(next, forBob.getRecord("Story", "Test"));
            assertFalse(forBob.handOut(new TestQuestProgression(), "Story", "Test", null, next));

            assertEquals(1, teams.getQuestIds(AMIS).size());
            AbstractQuestProgression<?> quest = harness.services().getQuestProgressionService().getQuest(teams.getQuestIds(AMIS).iterator().next());
            assertEquals(Set.of(alice, bob), Set.copyOf(quest.getPlayers()));
        }
    }

    @Test
    void aSoloPlayerGetsTheQuestInTheirPersonalTeam() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            UUID alice = harness.players().connect();

            Recording targets = reach(harness, "party", Occasion.connection(alice, harness.players().of(alice)));

            assertEquals(List.of(TeamKey.personal("party", alice)), targets.teams);
            assertEquals(List.of(alice), targets.joining);
        }
    }

    @Test
    void aMemberIsReachedThroughTheirTeam() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            UUID alice = harness.players().connect();
            harness.players().of(alice).ensureAndGetComponent(harness.players().types().questStore()).setTeam("party", "amis");

            Recording targets = reach(harness, "party", Occasion.connection(alice, harness.players().of(alice)));

            assertEquals(List.of(AMIS), targets.teams);
        }
    }

    @Test
    void aPlayerWithoutAFactionGetsNothing() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            UUID alice = harness.players().connect();

            Recording targets = reach(harness, "faction", Occasion.connection(alice, harness.players().of(alice)));

            assertTrue(targets.teams.isEmpty());
        }
    }

    @Test
    void aScheduleReachesTeamsWithAMemberOnline() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            UUID alice = harness.players().connect();
            UUID bob = harness.players().connect();
            UUID carol = harness.players().connect();
            harness.players().of(alice).ensureAndGetComponent(harness.players().types().questStore()).setTeam("faction", "lion");
            harness.players().of(bob).ensureAndGetComponent(harness.players().types().questStore()).setTeam("faction", "lion");
            harness.players().of(carol).ensureAndGetComponent(harness.players().types().questStore()).setTeam("faction", "wolf");
            harness.players().disconnect(carol);

            Recording targets = reach(harness, "faction", Occasion.period("period:2026-11-16T00:00:00Z"));

            assertEquals(Set.of(new TeamKey("faction", "lion")), Set.copyOf(targets.teams));
        }
    }

    @Nonnull
    private static Recording reach(@Nonnull QuestHarness harness, @Nonnull String teamType, @Nonnull Occasion occasion) {
        Recording targets = new Recording(harness);
        new TeamAssignmentScope(teamType).reach(assignment(), occasion, targets);
        return targets;
    }

    @Nonnull
    private static OpenQuestAssignment assignment() {
        OpenQuestAssignment assignment = new OpenQuestAssignment();
        assignment.id = "Story";
        assignment.trigger = new PlayerConnectTrigger();
        assignment.repeat = new OnceRepeat();
        return assignment;
    }

    /**
     * Keeps the teams a scope picks, the online players being the harness's.
     */
    private static final class Recording implements AssignmentTargets {
        private final QuestHarness harness;
        private final List<TeamKey> teams = new ArrayList<>();
        private final List<UUID> joining = new ArrayList<>();

        private Recording(@Nonnull QuestHarness harness) {
            this.harness = harness;
        }

        @Override
        public void team(@Nonnull TeamKey team, @Nullable UUID joining) {
            teams.add(team);
            if (joining != null) this.joining.add(joining);
        }

        @Override
        public void eachOnlinePlayer(@Nonnull BiConsumer<UUID, EntityComponents> action) {
            for (UUID playerId : harness.players().online()) action.accept(playerId, harness.players().of(playerId));
        }

        @Override
        public void player(@Nonnull UUID playerId, @Nonnull EntityComponents player) {}

        @Override
        public void onlinePlayers() {}

        @Override
        public void world(@Nonnull World world, @Nullable UUID joining) {}

        @Override
        public void group(@Nonnull String group, @Nullable UUID joining) {}

        @Override
        public void universe(@Nullable UUID joining) {}
    }
}
