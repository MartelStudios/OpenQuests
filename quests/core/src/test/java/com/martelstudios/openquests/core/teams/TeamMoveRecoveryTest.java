package com.martelstudios.openquests.core.teams;

import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.TestQuestAsset;
import com.martelstudios.openquests.core.persistence.*;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class TeamMoveRecoveryTest {
    @Test
    void releasingARemoteTeamKeepsTheHistoryAnOnlinePlayerAbandoned() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            UUID alice = harness.players().connect();
            TeamRecord team = harness.services().getTeams().create("faction", UUID.randomUUID(), "Remote owner", null);
            harness.services().getTeams().addMember(team.getId(), alice);
            TestQuestProgression quest = harness.register();
            new TeamQuestScope(team.getId()).share(harness.services().getScopeServices(), quest);
            quest.abandonPlayer(alice);
            harness.settle();

            harness.services().getTeamService().change(alice, "Alice", "faction", null);
            harness.settle();

            assertSame(quest, harness.services().getQuestProgressionService().getQuest(quest.getId()));
            assertTrue(quest.getAbandonedPlayers().contains(alice));
        }
    }
    @Test
    void aMoveForAnAbsentPlayerDoesNotKeepARemoteTeamLoadedHere() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            UUID absent = UUID.randomUUID();
            TeamRecord team = harness.services().getTeams().create("faction", UUID.randomUUID(), "Remote owner", null);
            TestQuestProgression quest = harness.register();
            new TeamQuestScope(team.getId()).share(harness.services().getScopeServices(), quest);
            harness.settle();

            harness.services().getTeamService().change(absent, "Absent", "faction", team.getId());
            harness.settle();
            // The release runs on the storage thread, and the unload it leaves is written on a later pass
            harness.settle();

            assertFalse(harness.services().getScopeIndexes().getLoadedKeys(TeamQuestService.TEAM_INDEX_PREFIX).contains(TeamQuestService.key(team.getId())));
            assertNull(harness.services().getQuestProgressionService().getQuest(quest.getId()));
            assertEquals(team.getId(), harness.services().getTeamService().currentTeamOf(absent, "faction"));
        }
    }
    @Test
    void acceptingAnInvitationRechecksItsExpiryBeforeLeavingTheOldTeam() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            UUID alice = connect(harness, "Alice");
            UUID bob = connect(harness, "Bob");
            var service = harness.services().getTeamService();
            UUID original = service.currentTeamOf(alice, "party");
            UUID destination = service.currentTeamOf(bob, "party");
            harness.services().getTeams().invite(destination, bob, alice);
            harness.clock().advance(java.time.Duration.ofHours(25));
            assertThrows(IllegalArgumentException.class, () -> service.accept(alice, "Alice", "party", destination));
            assertEquals(original, service.currentTeamOf(alice, "party"));
            assertEquals(1, harness.storage().loadTeamsForPlayer(alice).size());
        }
    }

    @Test
    void creatingANewTeamMovesItsOwnerWithoutAnIntermediateDoubleMembership() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            UUID alice = connect(harness, "Alice");
            TeamRecord created = harness.services().getTeamService().create(alice, "Alice", "party", "Explorers");
            assertEquals(List.of(created.getId()), harness.storage().loadTeamsForPlayer(alice).stream().map(TeamRecord::getId).toList());
            assertTrue(harness.storage().loadMessages(List.of(alice)).stream().anyMatch(message ->
                    message.getTeam() != null && created.getId().equals(message.getTeam().to())));
        }
    }

    @Test
    void aFailedCommitLeavesNoCopiedQuestOrNewTeamBehind() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            UUID alice = connect(harness, "Alice");
            UUID from = harness.services().getTeamService().currentTeamOf(alice, "party");
            TeamRecord original = harness.storage().loadTeam(from);
            var snapshot = new QuestProgressionRecord(harness.register());
            TeamRecord target = new TeamRecord(UUID.randomUUID(), "party", null, "Alice", alice, List.of(alice), List.of());
            PlayerMessage notification = PlayerMessage.team(alice, new TeamChange("party", List.of(from), target.getId(), List.of(snapshot), java.util.Map.of()));
            harness.storage().postMessage(notification);
            assertThrows(QuestStorageException.class, () -> harness.storage().transactTeams(all -> new TeamTransaction(
                    List.of(original.withMembers(List.of()).withVersion(original.getVersion() + 1), target), notification)));
            assertNull(harness.storage().loadProgression(snapshot.quest.getId()));
            assertNull(harness.storage().loadTeam(target.getId()));
            assertTrue(harness.storage().loadIndex(TeamQuestService.key(target.getId())).isEmpty());
            assertEquals(from, harness.services().getTeamService().currentTeamOf(alice, "party"));
        }
    }
    @Test
    void anOfflineDepartureCopiesTheActualTeamDespiteAnOutdatedPlayerRecord() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            UUID bob = connect(harness, "Bob");
            UUID alice = connect(harness, "Alice");
            var service = harness.services().getTeamService();
            UUID bobs = service.currentTeamOf(bob, "party");
            service.change(alice, "Alice", "party", bobs);
            TestQuestProgression source = harness.register();
            new TeamQuestScope(bobs).share(harness.services().getScopeServices(), source);
            harness.count(source, 3, 5);
            harness.settle();
            PlayerQuestRecord stale = new PlayerQuestRecord();
            stale.getTeams().put("party", UUID.randomUUID());
            harness.storage().savePlayer(alice, stale, List.of(), false);
            harness.players().disconnect(alice);

            service.change(alice, "Alice", "party", null);
            UUID own = service.currentTeamOf(alice, "party");
            assertNotEquals(bobs, own);
            var ids = harness.storage().loadIndex(TeamQuestService.key(own));
            assertEquals(1, ids.size());
            TestQuestProgression copy = (TestQuestProgression) harness.storage().loadProgression(ids.iterator().next());
            assertEquals(3, copy.getCounter());
            assertFalse(harness.services().getTeams().get(bobs).getMembers().contains(alice));
            assertEquals(1, harness.storage().loadTeams().stream().filter(team -> team.getMembers().contains(alice)).count());
        }
    }

    @Test
    void aPersistedDepartureRecoversItsCopyAfterTheSourceDisappears() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            UUID alice = connect(harness, "Alice");
            var services = harness.services();
            UUID from = services.getTeamService().currentTeamOf(alice, "party");
            TestQuestProgression quest = harness.register();
            new TeamQuestScope(from).share(services.getScopeServices(), quest);
            harness.count(quest, 3, 5);
            harness.settle();
            List<QuestProgressionRecord> snapshots = new QuestCopier(services.getQuestProgressionService()).snapshot(quest, alice)
                    .stream().map(QuestProgressionRecord::new).toList();
            UUID copyId = snapshots.getLast().quest.getId();
            TeamRecord destination = new TeamRecord(UUID.randomUUID(), "party", null, "Alice", alice, List.of(alice), List.of());
            TeamRecord original = harness.storage().loadTeam(from);
            PlayerMessage notification = PlayerMessage.team(alice, new TeamChange("party", List.of(from), destination.getId(), snapshots, java.util.Map.of()));
            harness.storage().transactTeams(all -> new TeamTransaction(List.of(original.withMembers(List.of()).withVersion(original.getVersion() + 1), destination), notification));

            // A different server finishes dissolving the old team before this player receives the move.
            services.getTeamQuestService().disband(from);
            harness.settle();
            services.getQuestPlayerStateService().prepareTeamChanges((player, change) -> { throw new IllegalStateException("temporary failure"); }, services.getTeamService()::isCurrent);
            services.getQuestPlayerStateService().takeInMessages();
            services.getQuestPlayerStateService().saveAllOnline(false);
            assertTrue(harness.storage().loadMessages(List.of(alice)).stream().anyMatch(message -> message.getId().equals(notification.getId())));

            services.getQuestPlayerStateService().prepareTeamChanges(services.getTeamService()::prepare, services.getTeamService()::isCurrent);
            services.getQuestPlayerStateService().takeInMessages();
            services.getQuestPlayerStateService().takeInMessages();
            assertEquals(java.util.Set.of(copyId), harness.storage().loadIndex(TeamQuestService.key(destination.getId())));
            assertEquals(3, ((TestQuestProgression) harness.storage().loadProgression(copyId)).getCounter());
            assertEquals(destination.getId(), services.getQuestPlayerStateService().getQuestStore(alice).getTeams().get("party"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aCompletionDuringRecoveryUsesCommittedMembership(boolean joining) {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            UUID bob = harness.players().connect();
            UUID alice = UUID.randomUUID();
            TeamRecord team = harness.services().getTeams().create("faction", bob, "Bob", null);
            if (!joining) harness.services().getTeams().addMember(team.getId(), alice);
            TestQuestProgression quest = harness.register(new TestQuestAsset().paying(new TestQuestReward("gold")));
            new TeamQuestScope(team.getId()).share(harness.services().getScopeServices(), quest);
            harness.settle();
            TeamRecord before = harness.storage().loadTeam(team.getId());
            PlayerMessage notification = PlayerMessage.team(alice, new TeamChange("faction", joining ? null : team.getId(), joining ? team.getId() : null));
            harness.storage().transactTeams(all -> new TeamTransaction(List.of(before.withMembers(joining ? List.of(bob, alice) : List.of(bob)).withVersion(before.getVersion() + 1)), notification));

            harness.count(quest, 5, 5);
            harness.settle();
            boolean owed = harness.storage().loadMessages(List.of(alice)).stream().anyMatch(message -> message.getOwed() != null && message.getOwed().getQuestId().equals(quest.getId()));
            assertEquals(joining, owed, "queued quest-binding effects must neither lose a newcomer's reward nor pay someone who already left");
        }
    }

    @Test
    void aDelayedCacheUpdateCannotUndoANewerMove() {
        try (QuestHarness harness = new QuestHarness()) {
            UUID alice = harness.players().connect();
            var holder = harness.players().of(alice);
            TeamChange older = new TeamChange("party", null, UUID.randomUUID());
            older.setRevision(10);
            TeamChange newer = new TeamChange("party", older.to(), UUID.randomUUID());
            newer.setRevision(11);
            harness.services().getTeamService().apply(alice, holder, newer);
            harness.services().getTeamService().apply(alice, holder, older);
            assertEquals(newer.to(), holder.ensureAndGetComponent(harness.players().types().questStore()).getTeams().get("party"));
        }
    }

    @Test
    void aFailedAsyncMoveIsReportedToItsCallerWithoutChangingMembership() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("party", true));
            UUID alice = connect(harness, "Alice");
            UUID original = harness.services().getTeamService().currentTeamOf(alice, "party");
            var result = harness.services().getTeamService().changeAsync(alice, "Alice", "party", UUID.randomUUID());
            assertThrows(CompletionException.class, result::join);
            assertEquals(original, harness.services().getTeamService().currentTeamOf(alice, "party"));
        }
    }

    private static UUID connect(QuestHarness harness, String name) {
        UUID player = harness.players().connect();
        harness.services().getQuestPlayerStateService().host(player, harness.players().of(player));
        harness.services().getTeamService().onConnect(player, name, harness.players().of(player));
        return player;
    }
}
