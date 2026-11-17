package com.martelstudios.openquests.core.scopes;

import com.hypixel.hytale.codec.ExtraInfo;
import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.teams.TeamKey;
import com.martelstudios.openquests.core.teams.TestTeamTypes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamQuestServiceTest {

    private static final TeamKey LION = new TeamKey("faction", "lion");

    @Test
    void aSharedQuestReachesEveryOnlineMember() {
        try (QuestHarness harness = new QuestHarness()) {
            TeamQuestService teams = harness.services().getTeamQuestService();
            UUID alice = harness.players().connect();
            UUID bob = harness.players().connect();
            teams.addMember(LION, alice);
            teams.addMember(LION, bob);

            TestQuestProgression quest = harness.register();
            new TeamQuestScope(LION).share(harness.services().getScopeServices(), quest);

            assertEquals(Set.of(alice, bob), Set.copyOf(quest.getPlayers()));
            assertEquals(2, quest.getPlayers().size());
            assertEquals(Set.of(quest.getId()), teams.getQuestIds(LION));
        }
    }

    @Test
    void aConnectingMemberJoinsTheTeamsRunningQuests() {
        try (QuestHarness harness = new QuestHarness()) {
            TeamQuestService teams = harness.services().getTeamQuestService();
            UUID alice = harness.players().connect();
            harness.players().of(alice).ensureAndGetComponent(harness.players().types().questStore()).setTeam("faction", "lion");
            teams.addMember(LION, alice);
            harness.players().disconnect(alice);

            TestQuestProgression quest = harness.register();
            new TeamQuestScope(LION).share(harness.services().getScopeServices(), quest);
            assertTrue(quest.getPlayers().isEmpty());

            harness.players().connect(alice);
            teams.joinRunningQuests(alice, harness.players().of(alice));

            assertEquals(List.of(alice), List.copyOf(quest.getPlayers()));
            assertTrue(harness.questsOf(alice).contains(quest.getId()));
        }
    }

    @Test
    void aPersonalTeamHoldsItsOwnerOnly() {
        try (QuestHarness harness = new QuestHarness()) {
            UUID id = UUID.randomUUID();

            assertEquals(Set.of(id), harness.services().getTeamQuestService().members(TeamKey.personal("party", id)));
        }
    }

    @Test
    void aPlayerIsInTheirPersonalTeamOfEveryTypeTheyNamedNoTeamOf() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            harness.teamTypes().add(TestTeamTypes.type("guild", true));
            UUID alice = harness.players().connect();
            harness.players().of(alice).ensureAndGetComponent(harness.players().types().questStore()).setTeam("guild", "smiths");

            List<TeamKey> teams = harness.services().getTeamQuestService().teamsOf(alice, harness.players().of(alice));

            assertEquals(Set.of(TeamKey.personal("party", alice), new TeamKey("guild", "smiths")), Set.copyOf(teams));
        }
    }

    @Test
    void aSharedFollowUpIsCreatedOnce() {
        try (QuestHarness harness = new QuestHarness()) {
            TeamQuestService teams = harness.services().getTeamQuestService();
            teams.addMember(LION, harness.players().connect());
            teams.addMember(LION, harness.players().connect());
            TestQuestProgression quest = harness.register();
            new TeamQuestScope(LION).share(harness.services().getScopeServices(), quest);

            harness.count(quest, 5, 5);
            TestQuestProgression followUp = harness.register();
            quest.getScope().share(harness.services().getScopeServices(), followUp);

            assertEquals(Set.of(followUp.getId()), teams.getQuestIds(LION));
            assertEquals(2, followUp.getPlayers().size());
        }
    }

    @Test
    void anEndedQuestLeavesTheIndex() {
        try (QuestHarness harness = new QuestHarness()) {
            TeamQuestService teams = harness.services().getTeamQuestService();
            teams.addMember(LION, harness.players().connect());
            TestQuestProgression quest = harness.register();
            new TeamQuestScope(LION).share(harness.services().getScopeServices(), quest);

            harness.count(quest, 5, 5);

            assertTrue(quest.isOver());
            assertTrue(teams.getQuestIds(LION).isEmpty());
            assertEquals(new TeamQuestScope(LION).getTeam(), ((TeamQuestScope) quest.getScope()).getTeam());
        }
    }

    @Test
    void aTeamScopeReadsBackItsTeam() {
        try (QuestHarness harness = new QuestHarness()) {
            TestQuestProgression quest = harness.register();
            new TeamQuestScope(LION).share(harness.services().getScopeServices(), quest);

            ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
            AbstractQuestProgression<?> read = AbstractQuestProgression.CODEC.decode(AbstractQuestProgression.CODEC.encode(quest, extraInfo), extraInfo);

            assertEquals(LION, ((TeamQuestScope) read.getScope()).getTeam());
        }
    }
}
