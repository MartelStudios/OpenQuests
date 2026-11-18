package com.martelstudios.openquests.core.scopes;

import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.teams.TestTeamTypes;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamUnloadTest {

    @Test
    void aQuestReadBackBeforeItsLastChangesAreWrittenKeepsThem() throws InterruptedException {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            UUID lion = harness.services().getTeams().create("faction", UUID.randomUUID(), "Alice", "Lions").getId();
            TestQuestProgression quest = harness.register();
            new TeamQuestScope(lion).share(harness.services().getScopeServices(), quest);
            harness.settle();
            QuestProgressionService quests = harness.services().getQuestProgressionService();

            // Holds the storage thread, so the write of the change waits behind it
            CountDownLatch held = new CountDownLatch(1);
            harness.services().getQuestSyncService().execute("holding the storage thread", () -> {
                try { held.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            quest.addToCounter(4);
            quests.unloadQuest(quest.getId());

            assertEquals(4, ((TestQuestProgression) quests.loadQuest(quest.getId())).getCounter());
            held.countDown();
            harness.settle();
            assertEquals(4, ((TestQuestProgression) harness.storage().loadProgression(quest.getId())).getCounter());
        }
    }

    @Test
    void aTeamWithNobodyOnlineIsUnloaded() {
        try (QuestHarness harness = new QuestHarness()) {
            harness.teamTypes().add(TestTeamTypes.type("faction", false));
            UUID alice = host(harness, harness.players().connect());
            UUID bob = host(harness, harness.players().connect());
            UUID lion = harness.services().getTeams().create("faction", alice, "Alice", "Lions").getId();
            harness.services().getTeams().addMember(lion, bob);
            for (UUID member : Set.of(alice, bob)) {
                harness.players().of(member).ensureAndGetComponent(harness.players().types().questStore()).setTeam("faction", lion);
            }
            new TeamQuestScope(lion).share(harness.services().getScopeServices(), harness.register());
            ScopeIndexes indexes = harness.services().getScopeIndexes();

            leave(harness, bob);
            assertEquals(Set.of(TeamQuestService.key(lion)), indexes.getLoadedKeys(TeamQuestService.TEAM_INDEX_PREFIX));

            leave(harness, alice);
            harness.settle();
            assertTrue(indexes.getLoadedKeys(TeamQuestService.TEAM_INDEX_PREFIX).isEmpty());
        }
    }

    @Nonnull
    private static UUID host(@Nonnull QuestHarness harness, @Nonnull UUID playerId) {
        harness.services().getQuestPlayerStateService().host(playerId, harness.players().of(playerId));
        return playerId;
    }

    private static void leave(@Nonnull QuestHarness harness, @Nonnull UUID playerId) {
        harness.players().disconnect(playerId);
        harness.services().getTeamService().onDisconnect(playerId);
    }
}
