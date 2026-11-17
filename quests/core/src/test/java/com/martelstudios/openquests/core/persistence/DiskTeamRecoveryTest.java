package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.persistence.disk.DiskQuestStorage;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.teams.TeamChange;
import com.martelstudios.openquests.core.teams.Teams;
import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.teams.TestTeamTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DiskTeamRecoveryTest {
    static {
        AbstractQuestProgression.CODEC.register(TestQuestProgression.TYPE, TestQuestProgression.class, TestQuestProgression.CODEC);
        QuestScope.CODEC.register(TeamQuestScope.TYPE, TeamQuestScope.class, TeamQuestScope.CODEC);
    }

    @TempDir Path folder;

    @Test
    void aPersonalDepartureSurvivesRestartingAllServices() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID original;
        UUID personal;
        UUID sourceId;
        try (var server = new QuestHarness(new DiskQuestStorage(folder))) {
            server.teamTypes().add(TestTeamTypes.type("party", true));
            connect(server, owner);
            original = server.services().getTeamService().currentTeamOf(owner, "party");
            server.services().getTeams().addMember(original, other);
            TestQuestProgression source = server.register();
            sourceId = source.getId();
            new TeamQuestScope(original).share(server.services().getScopeServices(), source);
            server.count(source, 3, 10);
            server.settle();
            server.services().getTeamService().changeAsync(owner, "Alice", "party", null).join();
            server.settle();
            personal = server.services().getTeamService().currentTeamOf(owner, "party");
            assertNotEquals(original, personal);
            server.services().getQuestPlayerStateService().saveAllOnline(true);
        }
        try (var server = new QuestHarness(new DiskQuestStorage(folder))) {
            server.teamTypes().add(TestTeamTypes.type("party", true));
            connect(server, owner);
            assertEquals(personal, server.services().getTeamService().currentTeamOf(owner, "party"));
            assertEquals(other, server.storage().loadTeam(original).getOwner());
            Set<UUID> ids = server.services().getTeamQuestService().getQuestIds(personal);
            assertEquals(1, ids.size());
            TestQuestProgression copy = (TestQuestProgression) server.services().getQuestProgressionService().getQuest(ids.iterator().next());
            assertEquals(3, copy.getCounter());
            assertTrue(server.questsOf(owner).contains(copy.getId()));
            server.count(copy, 2, 10);
            server.services().getQuestPlayerStateService().saveNow(owner);
            assertEquals(5, ((TestQuestProgression) server.storage().loadProgression(copy.getId())).getCounter());
            assertEquals(3, ((TestQuestProgression) server.storage().loadProgression(sourceId)).getCounter());
        }
    }

    @Test
    void legacyTeamFilesRemainReadable() throws Exception {
        UUID player = UUID.randomUUID();
        TeamRecord team = new TeamRecord(UUID.randomUUID(), "party", "Legacy", "Alice", player, List.of(player), List.of());
        Files.createDirectories(folder.resolve("teams"));
        Files.writeString(folder.resolve("teams").resolve(team.getId() + ".bson"), CodecJson.encode(TeamRecord.CODEC, team));
        try (var storage = open()) {
            assertEquals("Legacy", storage.loadTeamsForPlayer(player).getFirst().getName());
        }
    }

    @Test
    void anUnreadableJournalStopsStartupRatherThanDiscardingTheMove() throws Exception {
        Files.createDirectories(folder.resolve("team-transactions"));
        Files.writeString(folder.resolve("team-transactions").resolve(UUID.randomUUID() + ".json"), "invalid transaction");
        assertThrows(QuestStorageException.class, this::open);
    }

    enum Interruption { BEFORE_COPIES, BEFORE_HISTORY, BEFORE_MESSAGE, AFTER_MESSAGE }

    @ParameterizedTest
    @EnumSource(Interruption.class)
    void restartCompletesTheMoveWithoutDuplicatingOrRewindingCopies(Interruption interruption) throws Exception {
        UUID player = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        TeamRecord source = new TeamRecord(UUID.randomUUID(), "party", null, "Alice", player, List.of(player, other), List.of());
        TeamRecord destination = new TeamRecord(UUID.randomUUID(), "party", null, "Alice", player, List.of(player), List.of());
        TestQuestProgression copy = new TestQuestProgression().setCounter(3).join(player);
        AssignmentRecord inherited = new AssignmentRecord(1, 1234L, "connection");
        TeamChange change = new TeamChange("party", List.of(source.getId()), destination.getId(),
                List.of(new QuestProgressionRecord(copy)), Map.of("story", Map.of("intro", inherited)));
        PlayerMessage message = PlayerMessage.team(player, change);

        try (var storage = new InterruptedStorage(folder, interruption)) {
            storage.start();
            storage.createTeam(source);
            storage.addToIndex(Teams.invitationsKey(player), List.of(destination.getId()));
            storage.interrupt = true;
            assertThrows(QuestStorageException.class, () -> storage.transactTeams(player, null, all ->
                    new TeamTransaction(List.of(source.withMembers(List.of(other)).withOwner(other).withVersion(1), destination), message)
                            .removeFromIndex(Teams.invitationsKey(player), destination.getId())));
            // The durable intent must remain until every effect, including the outbox, succeeds.
            try (var intents = Files.list(folder.resolve("team-transactions"))) {
                assertEquals(1, intents.filter(path -> path.toString().endsWith(".json")).count());
            }
            if (interruption != Interruption.BEFORE_COPIES) {
                TestQuestProgression advanced = (TestQuestProgression) storage.loadProgression(copy.getId());
                advanced.setCounter(4);
                storage.writeProgressions(List.of(advanced));
            }
            if (interruption == Interruption.BEFORE_MESSAGE || interruption == Interruption.AFTER_MESSAGE) {
                assertTrue(storage.claimAssignment(TeamQuestService.key(destination.getId()), "story", "intro", inherited,
                        new AssignmentRecord(2, 2345L, "next-connection")));
            }
        }

        try (var storage = open()) {
            assertEquals(List.of(destination.getId()), storage.loadTeamsForPlayer(player).stream().map(TeamRecord::getId).toList());
            assertEquals(List.of(other), storage.loadTeam(source.getId()).getMembers());
            assertEquals(other, storage.loadTeam(source.getId()).getOwner());
            assertEquals(Set.of(copy.getId()), storage.loadIndex(TeamQuestService.key(destination.getId())));
            assertTrue(storage.loadIndex(Teams.invitationsKey(player)).isEmpty());
            TestQuestProgression restored = (TestQuestProgression) storage.loadProgression(copy.getId());
            assertEquals(interruption == Interruption.BEFORE_COPIES ? 3 : 4, restored.getCounter());
            assertEquals(destination.getId(), ((TeamQuestScope) restored.getScope()).getTeamId());
            assertEquals(Set.of(player), restored.getPlayers());
            AssignmentRecord expected = interruption == Interruption.BEFORE_MESSAGE || interruption == Interruption.AFTER_MESSAGE
                    ? new AssignmentRecord(2, 2345L, "next-connection") : inherited;
            assertEquals(expected, storage.loadAssignments(TeamQuestService.key(destination.getId())).get("story", "intro"));
            var messages = storage.loadMessages(List.of(player));
            assertEquals(1, messages.size());
            assertEquals(message.getId(), messages.getFirst().getId());
            assertTrue(messages.getFirst().getTeam().revision() > 0);
        }
        try (var storage = open()) {
            assertEquals(1, storage.loadAllProgressions().size());
            assertEquals(1, storage.loadMessages(List.of(player)).size());
            try (var intents = Files.list(folder.resolve("team-transactions"))) {
                assertEquals(0, intents.filter(path -> path.toString().endsWith(".json")).count());
            }
        }
    }

    @Test
    void membershipsSurviveAStorageRestart() {
        UUID player = UUID.randomUUID();
        TeamRecord team = new TeamRecord(UUID.randomUUID(), "party", null, "Alice", player, List.of(player), List.of());
        try (var storage = open()) {
            assertTrue(storage.createTeam(team));
        }
        try (var storage = open()) {
            assertEquals(List.of(team.getId()), storage.loadTeamsForPlayer(player).stream().map(TeamRecord::getId).toList());
        }
    }

    private DiskQuestStorage open() {
        var storage = new DiskQuestStorage(folder);
        storage.start();
        return storage;
    }

    private static void connect(QuestHarness server, UUID player) {
        server.players().connect(player);
        server.services().getQuestPlayerStateService().host(player, server.players().of(player));
        server.services().getTeamService().onConnect(player, "Alice", server.players().of(player));
    }

    private static final class InterruptedStorage extends DiskQuestStorage {
        private final Interruption interruption;
        private boolean interrupt;

        InterruptedStorage(Path folder, Interruption interruption) {
            super(folder);
            this.interruption = interruption;
        }

        private void failAt(Interruption stage) {
            if (!interrupt || interruption != stage) return;
            interrupt = false;
            throw new QuestStorageException("Simulated process interruption at " + stage);
        }

        @Override
        public Set<UUID> writeProgressions(Collection<? extends AbstractQuestProgression<?>> quests) {
            failAt(Interruption.BEFORE_COPIES);
            return super.writeProgressions(quests);
        }

        @Override
        public boolean claimAssignment(String holder, String assignment, String asset, AssignmentRecord expected, AssignmentRecord next) {
            failAt(Interruption.BEFORE_HISTORY);
            return super.claimAssignment(holder, assignment, asset, expected, next);
        }

        @Override
        public void postMessage(PlayerMessage message) {
            failAt(Interruption.BEFORE_MESSAGE);
            super.postMessage(message);
            failAt(Interruption.AFTER_MESSAGE);
        }
    }
}
