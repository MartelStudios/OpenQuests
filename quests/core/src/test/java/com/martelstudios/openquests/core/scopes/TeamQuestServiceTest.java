package com.martelstudios.openquests.core.scopes;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.TestQuestAsset;
import com.martelstudios.openquests.core.constraints.AssignmentRequest;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.teams.TeamService;
import com.martelstudios.openquests.core.teams.Teams;
import com.martelstudios.openquests.core.teams.TestTeamTypes;
import com.martelstudios.openquests.core.utils.EntityComponents;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamQuestServiceTest {

    static {
        QuestReward.CODEC.register("TeamReconnectTest", ReconnectReward.class, ReconnectReward.CODEC);
    }

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
    void aConnectingHolderReceivesQuestsBeforeTheUniverseListsThePlayerOnline() {
        UUID alice = harness.players().connect();
        UUID bob = UUID.randomUUID();
        UUID teamId = team("faction", alice, bob);
        TestQuestProgression quest = share(teamId, harness.register());
        assertTrue(harness.questsOf(bob).isEmpty());
        assertFalse(harness.players().isOnline(bob));

        teamService.onConnect(bob, "Bob", harness.players().of(bob));

        assertTrue(harness.questsOf(bob).contains(quest.getId()));
    }

    @Test
    void anOfflineMembersStoredEntityIsLeftAlone() {
        UUID alice = harness.players().connect();
        UUID bob = UUID.randomUUID();
        UUID teamId = team("faction", alice, bob);

        share(teamId, harness.register());

        assertTrue(harness.questsOf(bob).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void anOfflineMemberCollectsACompletedQuestsRewardsOnConnection(boolean autoClaim) {
        UUID alice = harness.players().connect();
        UUID bob = UUID.randomUUID();
        UUID teamId = team("faction", alice, bob);
        ReconnectReward original = new ReconnectReward();
        original.automatic(autoClaim);
        TestQuestProgression quest = share(teamId, harness.register(new TestQuestAsset().paying(original)));

        harness.count(quest, 5, 5);
        harness.settle();

        assertTrue(quest.getPlayers().contains(bob));
        assertTrue(harness.questsOf(bob).isEmpty());
        assertTrue(teamQuests.getQuestIds(teamId).isEmpty(), "connection cannot rely on the running team index");
        assertEquals(autoClaim ? 1 : 0, original.grants, "only Alice can collect before Bob connects");
        assertTrue(harness.storage().loadMessages(Set.of(bob)).stream().anyMatch(message -> message.getOwed() != null));

        // The connect event supplies the holder before the player is online in the universe.
        EntityComponents holder = harness.players().of(bob);
        var state = harness.services().getQuestPlayerStateService();
        var rewards = harness.services().getQuestRewardService();
        state.host(bob, holder);
        assertTrue(rewards.isOwed(quest.getId(), holder));
        var pending = rewards.getPending(holder).get(quest.getId());
        ReconnectReward restored = (ReconnectReward) pending.getRewards()[0];
        assertEquals(0, restored.grants);

        harness.players().connect(bob);
        // World entry can receive a copied holder; the settled state still has to reach the connection session.
        EntityComponents worldHolder = harness.players().copyOf(bob);
        rewards.claimAutoOnWorldEntry(bob, worldHolder);
        assertEquals(autoClaim ? 1 : 0, restored.grants);
        if (!autoClaim) assertTrue(rewards.claim(quest.getId(), holder));
        assertEquals(1, restored.grants);
        assertFalse(rewards.isOwed(quest.getId(), autoClaim ? worldHolder : holder));

        if (autoClaim) harness.settle();
        else state.saveNow(bob);
        assertTrue(harness.storage().loadMessages(Set.of(bob)).isEmpty());
        assertTrue(harness.storage().loadPlayer(bob).getPendingRewards().isEmpty());
        state.host(bob, holder);
        rewards.claimAuto(holder);
        assertFalse(rewards.claim(quest.getId(), holder));
        assertEquals(1, restored.grants);
    }

    /** A serializable reward whose successful delivery can be observed after the storage round trip. */
    public static final class ReconnectReward extends QuestReward {
        static final BuilderCodec<ReconnectReward> CODEC = BuilderCodec.builder(
                ReconnectReward.class, ReconnectReward::new, QuestReward.BASE_CODEC).build();

        private int grants;

        public ReconnectReward() {}

        void automatic(boolean value) {
            autoClaim = value;
        }

        @Override
        public boolean grant(@Nonnull OpenQuestsServices services, @Nonnull UUID sourceQuestId, @Nonnull EntityComponents player) {
            grants++;
            return true;
        }
    }

    @Test
    void aQuestHandedToAGoneTeamIsDropped() {
        UUID gone = UUID.randomUUID();

        TestQuestProgression quest = share(gone, harness.register());

        assertTrue(teamQuests.getQuestIds(gone).isEmpty());
        assertNull(harness.services().getQuestProgressionService().getQuest(quest.getId()));
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
    void aQuestAnotherServerSharedIsBroughtAgainAfterAFailure() {
        UUID bob = harness.players().connect();
        UUID teamId = team("faction", bob);
        teamQuests.getQuestIds(teamId);
        TestQuestProgression quest = harness.register();
        quest.apply(new AbstractQuestProgression.SetScope(new TeamQuestScope(teamId)));
        quest.apply(new AbstractQuestProgression.Join(bob));
        harness.storage().addToIndex(TeamQuestService.key(teamId), List.of(quest.getId()));
        ScopeIndexFollower follower = new ScopeIndexFollower(harness.services().getScopeIndexes(), harness.services().getUniverseQuestService(),
                harness.services().getWorldGroupIndex(), new FailingOnce(harness));

        try {
            follower.follow();
        } catch (RuntimeException ignored) {
            // Before the fix the failure escaped; the ids it absorbed are what matters
        }
        follower.follow();

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
     * Fails the first time it brings in what another server shared, as a storage blip would.
     */
    private static final class FailingOnce extends TeamQuestService {
        private boolean failed;

        FailingOnce(@Nonnull QuestHarness harness) {
            super(harness.services().getScopeIndexes(), harness.services().getQuestProgressionService(), harness.players(), harness.services().getTeams(), harness.services().getPlayerQuestService());
        }

        @Override
        public void joinAdded(@Nonnull UUID teamId, @Nonnull Collection<UUID> questIds) {
            if (!failed) {
                failed = true;
                throw new IllegalStateException("storage blip");
            }
            super.joinAdded(teamId, questIds);
        }
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
