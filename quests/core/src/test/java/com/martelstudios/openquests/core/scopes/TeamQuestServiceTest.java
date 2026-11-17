package com.martelstudios.openquests.core.scopes;

import com.hypixel.hytale.codec.ExtraInfo;
import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.TestQuestAsset;
import com.martelstudios.openquests.core.constraints.AssignmentRequest;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.teams.TeamService;
import com.martelstudios.openquests.core.teams.Teams;
import com.martelstudios.openquests.core.teams.TestTeamTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamQuestServiceTest {

    private QuestHarness harness;
    private Teams teams;
    private TeamQuestService teamQuests;
    private TeamService teamService;

    @BeforeEach
    void open() {
        harness = new QuestHarness();
        harness.teamTypes().add(TestTeamTypes.type("party", true));
        harness.teamTypes().add(TestTeamTypes.type("faction", false));
        teams = harness.services().getTeams();
        teamQuests = harness.services().getTeamQuestService();
        teamService = harness.services().getTeamService();
    }

    @AfterEach
    void close() {
        harness.close();
    }

    @Test
    void aSharedQuestReachesEveryMember() {
        UUID alice = harness.players().connect();
        UUID bob = harness.players().connect();
        UUID teamId = team("faction", alice, bob);

        TestQuestProgression quest = share(teamId, harness.register());

        assertEquals(Set.of(alice, bob), Set.copyOf(quest.getPlayers()));
        assertEquals(2, quest.getPlayers().size());
        assertEquals(Set.of(quest.getId()), teamQuests.getQuestIds(teamId));
        assertTrue(harness.questsOf(alice).contains(quest.getId()));
    }

    @Test
    void anOfflineMemberIsBoundToANewQuest() {
        UUID alice = harness.players().connect();
        UUID bob = UUID.randomUUID();
        UUID teamId = team("faction", alice, bob);

        TestQuestProgression quest = share(teamId, harness.register());

        assertTrue(quest.getPlayers().contains(bob));
    }

    @Test
    void aMemberBoundOnAnotherServerSeesTheQuestHere() {
        UUID bob = harness.players().connect();
        UUID teamId = team("faction", bob);
        TestQuestProgression quest = harness.register();
        // What reading back a quest another server shared and bound bob to looks like
        quest.apply(new AbstractQuestProgression.SetScope(new TeamQuestScope(teamId)));
        quest.apply(new AbstractQuestProgression.Join(bob));

        teamQuests.joinAdded(teamId, List.of(quest.getId()));

        assertTrue(harness.questsOf(bob).contains(quest.getId()));
    }

    @Test
    void aConnectingMemberJoinsDespiteTheConstraints() {
        UUID alice = harness.players().connect();
        UUID teamId = team("faction", alice);
        TestQuestProgression quest = share(teamId, harness.register(new TestQuestAsset(new Refusing())));
        UUID bob = UUID.randomUUID();
        teams.addMember(teamId, bob);

        harness.players().connect(bob);
        questStore(bob).setTeam("faction", teamId);
        teamService.onConnect(bob, "Bob", harness.players().of(bob));

        assertTrue(quest.getPlayers().contains(bob));
        assertTrue(harness.questsOf(bob).contains(quest.getId()));
    }

    @Test
    void aConnectingPlayerGetsTheirPersonalTeams() {
        UUID alice = harness.players().connect();

        teamService.onConnect(alice, "Alice", harness.players().of(alice));

        Map<String, UUID> named = questStore(alice).getTeams();
        assertEquals(Set.of("party"), named.keySet());
        TeamRecord party = teams.get(named.get("party"));
        assertEquals(alice, party.getOwner());
        assertEquals(List.of(alice), party.getMembers());
        assertNull(party.getName());
        assertEquals("Alice", party.getNamedAfter());
    }

    @Test
    void aRecordNamingATeamThatDroppedThemIsCorrected() {
        UUID alice = harness.players().connect();
        UUID lion = team("faction", UUID.randomUUID());
        questStore(alice).setTeam("faction", lion);
        UUID party = teams.create("party", alice, "Alice", null).getId();
        questStore(alice).setTeam("party", party);

        teamService.onConnect(alice, "Alice", harness.players().of(alice));

        assertEquals(Map.of("party", party), questStore(alice).getTeams());
    }

    @Test
    void aSharedFollowUpIsCreatedOnce() {
        UUID teamId = team("faction", harness.players().connect(), harness.players().connect());
        TestQuestProgression quest = share(teamId, harness.register());

        harness.count(quest, 5, 5);
        harness.settle();
        TestQuestProgression followUp = harness.register();
        quest.getScope().share(harness.services().getScopeServices(), followUp);

        assertEquals(Set.of(followUp.getId()), teamQuests.getQuestIds(teamId));
        assertEquals(2, followUp.getPlayers().size());
    }

    @Test
    void anEndedQuestLeavesTheIndex() {
        UUID teamId = team("faction", harness.players().connect());
        TestQuestProgression quest = share(teamId, harness.register());

        harness.count(quest, 5, 5);
        harness.settle();

        assertTrue(quest.isOver());
        assertTrue(teamQuests.getQuestIds(teamId).isEmpty());
        assertEquals(teamId, ((TeamQuestScope) quest.getScope()).getTeamId());
    }

    @Test
    void aTeamScopeReadsBackItsTeam() {
        UUID teamId = team("faction", harness.players().connect());
        TestQuestProgression quest = share(teamId, harness.register());

        ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
        AbstractQuestProgression<?> read = AbstractQuestProgression.CODEC.decode(AbstractQuestProgression.CODEC.encode(quest, extraInfo), extraInfo);

        assertEquals(teamId, ((TeamQuestScope) read.getScope()).getTeamId());
    }

    @Nonnull
    private UUID team(@Nonnull String type, @Nonnull UUID owner, @Nonnull UUID... others) {
        UUID teamId = teams.create(type, owner, "Owner", "Lions").getId();
        for (UUID other : others) teams.addMember(teamId, other);
        return teamId;
    }

    @Nonnull
    private TestQuestProgression share(@Nonnull UUID teamId, @Nonnull TestQuestProgression quest) {
        new TeamQuestScope(teamId).share(harness.services().getScopeServices(), quest);
        return quest;
    }

    @Nonnull
    private QuestStoreComponent questStore(@Nonnull UUID playerId) {
        return harness.players().of(playerId).ensureAndGetComponent(harness.players().types().questStore());
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
