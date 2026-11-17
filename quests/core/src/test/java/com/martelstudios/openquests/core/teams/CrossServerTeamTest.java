package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.TestQuestAsset;
import com.martelstudios.openquests.core.persistence.PlayerMessage;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.persistence.jdbc.JdbcQuestStorage;
import com.martelstudios.openquests.core.persistence.jdbc.SqlDialect;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.utils.EntityComponents;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Independent service graphs, storage workers and hosting leases sharing one database. */
class CrossServerTeamTest {
    static {
        QuestReward.CODEC.register("CrossServerDelivery", DeliveredReward.class, DeliveredReward.CODEC);
    }

    private final String prefix = "oqservers_" + UUID.randomUUID().toString().substring(0, 8) + "_";

    @ParameterizedTest
    @ValueSource(strings = {"h2", "configured"})
    void concurrentMovesAndReversedNotificationsConvergeOnTheLastCommittedTeam(String backend) throws Exception {
        String url = url(backend);
        try (var first = server(url); var second = server(url); var workers = Executors.newFixedThreadPool(2)) {
            UUID player = connect(first, UUID.randomUUID(), "Alice");
            UUID left = first.services().getTeams().create("party", UUID.randomUUID(), "Left", null).getId();
            UUID right = second.services().getTeams().create("party", UUID.randomUUID(), "Right", null).getId();
            first.settle();
            first.services().getQuestPlayerStateService().takeInMessages();
            first.services().getQuestPlayerStateService().saveNow(player);

            CountDownLatch start = new CountDownLatch(1);
            var a = workers.submit(() -> { await(start); first.services().getTeamService().changeAsync(player, "Alice", "party", left).join(); });
            var b = workers.submit(() -> { await(start); second.services().getTeamService().changeAsync(player, "Alice", "party", right).join(); });
            start.countDown();
            a.get(10, TimeUnit.SECONDS);
            b.get(10, TimeUnit.SECONDS);
            first.settle();
            second.settle();

            var notifications = second.storage().loadMessages(List.of(player)).stream().filter(message -> message.getTeam() != null)
                    .sorted(Comparator.comparingLong((PlayerMessage message) -> message.getTeam().revision()).reversed()).toList();
            assertEquals(2, notifications.size());
            UUID committed = first.services().getTeamService().currentTeamOf(player, "party");
            assertEquals(notifications.getFirst().getTeam().to(), committed);
            first.services().getQuestPlayerStateService().deliverMessages(notifications);
            first.settle();
            assertEquals(committed, first.services().getQuestPlayerStateService().getQuestStore(player).getTeams().get("party"));
            assertEquals(1, second.storage().loadTeamsForPlayer(player).size());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "configured"})
    void anInvitationAcceptedOnAnotherServerUsesCurrentOwnershipAndExpiry(String backend) {
        String url = url(backend);
        try (var first = server(url); var second = server(url)) {
            UUID owner = connect(first, UUID.randomUUID(), "Owner");
            UUID player = connect(second, UUID.randomUUID(), "Alice");
            UUID destination = first.services().getTeamService().currentTeamOf(owner, "party");
            first.services().getTeams().invite(destination, owner, player);
            second.services().getTeamService().execute(() -> second.services().getTeamService().accept(player, "Alice", "party", destination)).join();
            assertEquals(List.of(owner, player), first.storage().loadTeam(destination).getMembers());
            assertTrue(first.storage().loadTeam(destination).getInvitations().isEmpty());
            assertTrue(second.storage().loadIndex(Teams.invitationsKey(player)).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> second.services().getTeams().rename(destination, player, "Forbidden"));
            first.services().getTeams().invite(destination, owner, UUID.randomUUID());
            UUID late = UUID.randomUUID();
            first.services().getTeams().invite(destination, owner, late);
            second.clock().advance(java.time.Duration.ofHours(25));
            assertThrows(IllegalArgumentException.class, () -> second.services().getTeamService().accept(late, "Late", "party", destination));
            assertTrue(first.storage().loadTeamsForPlayer(late).isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "configured"})
    void anOwnerLeavingOnAnotherServerGetsAnIndependentCopyAndPassesOwnership(String backend) {
        String url = url(backend);
        try (var first = server(url); var second = server(url)) {
            UUID owner = connect(first, UUID.randomUUID(), "Alice");
            UUID other = UUID.randomUUID();
            UUID originalTeam = first.services().getTeamService().currentTeamOf(owner, "party");
            first.services().getTeams().addMember(originalTeam, other);
            TestQuestProgression source = first.register();
            new TeamQuestScope(originalTeam).share(first.services().getScopeServices(), source);
            first.count(source, 3, 10);
            first.services().getQuestSyncService().writeSoon(source);
            first.settle();
            assertEquals(Set.of(source.getId()), second.storage().loadIndex(TeamQuestService.key(originalTeam)), "source index must be durable before the remote move");
            assertTrue(second.storage().loadProgression(source.getId()).getPlayers().contains(owner), "source membership must be durable before the remote move");

            second.services().getTeamService().changeAsync(owner, "Alice", "party", null).join();
            UUID personal = second.services().getTeamService().currentTeamOf(owner, "party");
            assertNotEquals(originalTeam, personal);
            assertEquals(other, first.storage().loadTeam(originalTeam).getOwner());
            assertEquals(List.of(other), first.storage().loadTeam(originalTeam).getMembers());
            Set<UUID> copied = second.storage().loadIndex(TeamQuestService.key(personal));
            assertEquals(1, copied.size());
            UUID copyId = copied.iterator().next();
            assertEquals(3, ((TestQuestProgression) first.storage().loadProgression(copyId)).getCounter());

            first.services().getQuestPlayerStateService().takeInMessages();
            first.settle();
            var copy = (TestQuestProgression) first.services().getQuestProgressionService().getQuest(copyId);
            assertNotNull(copy);
            first.count(copy, 2, 10);
            first.services().getQuestSyncService().writeSoon(copy);
            first.settle();
            assertEquals(5, ((TestQuestProgression) second.storage().loadProgression(copyId)).getCounter());
            assertEquals(3, ((TestQuestProgression) second.storage().loadProgression(source.getId())).getCounter());
            assertEquals(Set.of(owner), copy.getPlayers());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "configured"})
    void offlineRewardsCanBeCollectedAfterTransferringToAnotherServerOnlyOnce(String backend) {
        String url = url(backend);
        UUID player = UUID.randomUUID();
        UUID questId;
        try (var first = server(url)) {
            connect(first, player, "Alice");
            UUID team = first.services().getTeamService().currentTeamOf(player, "party");
            TestQuestProgression quest = first.register(new TestQuestAsset().paying(new DeliveredReward()));
            questId = quest.getId();
            new TeamQuestScope(team).share(first.services().getScopeServices(), quest);
            first.settle();
            first.services().getQuestPlayerStateService().saveAllOnline(true);
            first.players().disconnect(player);
            first.count(quest, 5, 5);
            first.settle();
            assertTrue(first.storage().loadMessages(List.of(player)).stream().anyMatch(message -> message.getOwed() != null));
        }
        try (var second = server(url)) {
            connect(second, player, "Alice");
            second.settle();
            var rewardService = second.services().getQuestRewardService();
            EntityComponents holder = second.players().of(player);
            assertTrue(rewardService.isOwed(questId, holder));
            var reward = (DeliveredReward) rewardService.getPending(holder).get(questId).getRewards()[0];
            assertTrue(rewardService.claim(questId, holder));
            assertEquals(1, reward.grants);
            second.services().getQuestPlayerStateService().saveAllOnline(true);
            assertTrue(second.storage().loadMessages(List.of(player)).isEmpty());
        }
        try (var third = server(url)) {
            connect(third, player, "Alice");
            assertFalse(third.services().getQuestRewardService().isOwed(questId, third.players().of(player)));
            assertFalse(third.services().getQuestRewardService().claim(questId, third.players().of(player)));
        }
    }

    private QuestHarness server(String url) {
        boolean configured = url.equals(System.getProperty("openquests.jdbc.url"));
        var storage = new JdbcQuestStorage(new JdbcQuestStorage.JdbcSettings(url,
                configured ? System.getProperty("openquests.jdbc.user") : null,
                configured ? System.getProperty("openquests.jdbc.password") : null,
                null, null, prefix, 4, 5, true, SqlDialect.fromUrl(url)));
        var harness = new QuestHarness(storage);
        harness.teamTypes().add(TestTeamTypes.type("party", true));
        return harness;
    }

    private String url(String backend) {
        if (backend.equals("h2")) return "jdbc:h2:mem:team-servers-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        String configured = System.getProperty("openquests.jdbc.url", "");
        Assumptions.assumeFalse(configured.isBlank(), "No external database configured");
        return configured;
    }

    private static UUID connect(QuestHarness server, UUID player, String name) {
        server.services().install();
        server.players().connect(player);
        server.services().getQuestPlayerStateService().host(player, server.players().of(player));
        server.services().getTeamService().onConnect(player, name, server.players().of(player));
        return player;
    }

    private static void await(CountDownLatch start) {
        try { assertTrue(start.await(5, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }

    public static final class DeliveredReward extends QuestReward {
        static final BuilderCodec<DeliveredReward> CODEC = BuilderCodec.builder(DeliveredReward.class, DeliveredReward::new, QuestReward.BASE_CODEC).build();
        int grants;

        public DeliveredReward() { autoClaim = false; }

        @Override
        public boolean grant(OpenQuestsServices services, UUID quest, EntityComponents player) {
            grants++;
            return true;
        }
    }
}
