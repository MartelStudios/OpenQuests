package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.SqlDialect;
import com.martelstudios.openquests.core.teams.TeamChange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TeamTransactionTest {
    @TempDir Path folder;
    private final String prefix = "oqmove_" + UUID.randomUUID().toString().substring(0, 8) + "_";

    @Test
    void aMoveWorksWithASingleConnectionIncludingItsPreparationReads() {
        try (var storage = open(url("h2"), 1)) {
            UUID player = UUID.randomUUID();
            TeamRecord original = team(player);
            storage.createTeam(original);
            storage.transactTeams(player, original.getId(), all -> {
                assertNotNull(storage.loadTeam(original.getId()));
                assertTrue(storage.loadAssignments("team:" + original.getId()).isEmpty());
                return new TeamTransaction(List.of(original.withName("Renamed").withVersion(1)),
                        PlayerMessage.team(player, new TeamChange("party", null, original.getId())));
            });
            assertEquals("Renamed", storage.loadTeamsForPlayer(player).getFirst().getName());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "sqlite", "configured"})
    void aFailedNotificationRollsBackBothMembershipWrites(String backend) {
        try (var storage = open(url(backend))) {
            UUID player = UUID.randomUUID();
            TeamRecord from = team(player);
            TeamRecord to = team(UUID.randomUUID());
            storage.createTeam(from);
            storage.createTeam(to);
            PlayerMessage notification = PlayerMessage.team(player, new TeamChange("party", from.getId(), to.getId()));
            storage.postMessage(notification); // Duplicate outbox id fails after both document writes.

            assertThrows(QuestStorageException.class, () -> storage.transactTeams(all -> new TeamTransaction(List.of(
                    from.withMembers(List.of()).withVersion(1), to.withMembers(List.of(to.getOwner(), player)).withVersion(1)), notification)));

            assertEquals(List.of(player), storage.loadTeam(from.getId()).getMembers());
            assertEquals(List.of(to.getOwner()), storage.loadTeam(to.getId()).getMembers());
            assertEquals(0, storage.loadTeam(from.getId()).getVersion());
            assertEquals(from.getId(), storage.loadTeamsForPlayer(player).getFirst().getId());
            assertEquals(1, storage.loadMessages(List.of(player)).size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "sqlite", "configured"})
    void twoServersMovingOnePlayerLeaveExactlyOneMembership(String backend) throws Exception {
        String url = url(backend);
        try (var first = open(url); var second = open(url); var workers = Executors.newFixedThreadPool(2)) {
            UUID player = UUID.randomUUID();
            TeamRecord origin = team(player);
            TeamRecord left = team(UUID.randomUUID());
            TeamRecord right = team(UUID.randomUUID());
            first.createTeam(origin);
            first.createTeam(left);
            first.createTeam(right);
            CountDownLatch start = new CountDownLatch(1);
            var a = workers.submit(() -> { await(start); transfer(first, player, left.getId()); });
            var b = workers.submit(() -> { await(start); transfer(second, player, right.getId()); });
            start.countDown();
            a.get(5, TimeUnit.SECONDS);
            b.get(5, TimeUnit.SECONDS);

            assertEquals(1, first.loadTeamsForPlayer(player).size());
            var messages = first.loadMessages(List.of(player));
            assertEquals(2, messages.size());
            assertNotEquals(messages.get(0).getTeam().revision(), messages.get(1).getTeam().revision());
            assertTrue(messages.stream().allMatch(message -> message.getTeam().revision() > 0));
        }
    }

    private static void transfer(QuestStorage storage, UUID player, UUID destination) {
        storage.transactTeams(all -> {
            List<TeamRecord> writes = new ArrayList<>();
            UUID source = null;
            for (TeamRecord team : all) {
                if (team.getId().equals(destination)) {
                    List<UUID> members = new ArrayList<>(team.getMembers());
                    members.add(player);
                    writes.add(team.withMembers(members).withVersion(team.getVersion() + 1));
                } else if (team.getMembers().contains(player)) {
                    source = team.getId();
                    writes.add(team.withMembers(team.getMembers().stream().filter(id -> !id.equals(player)).toList()).withVersion(team.getVersion() + 1));
                }
            }
            return new TeamTransaction(writes, PlayerMessage.team(player, new TeamChange("party", source, destination)));
        });
    }

    private static void await(CountDownLatch latch) {
        try { assertTrue(latch.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }

    private String url(String backend) {
        if (backend.equals("h2")) return "jdbc:h2:mem:team-atomic-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        if (backend.equals("sqlite")) return "jdbc:sqlite:" + folder.resolve("teams.db");
        String configured = System.getProperty("openquests.jdbc.url", "");
        Assumptions.assumeFalse(configured.isBlank(), "No external database configured");
        return configured;
    }

    private JdbcQuestStorage open(String url) {
        return open(url, 4);
    }

    private JdbcQuestStorage open(String url, int poolSize) {
        boolean configured = url.equals(System.getProperty("openquests.jdbc.url"));
        var storage = new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(url,
                configured ? System.getProperty("openquests.jdbc.user") : null,
                configured ? System.getProperty("openquests.jdbc.password") : null,
                null, null, prefix, poolSize, 5, true, SqlDialect.fromUrl(url)));
        storage.start();
        return storage;
    }

    private static TeamRecord team(UUID owner) {
        return new TeamRecord(UUID.randomUUID(), "party", null, "Owner", owner, List.of(owner), List.of());
    }
}
