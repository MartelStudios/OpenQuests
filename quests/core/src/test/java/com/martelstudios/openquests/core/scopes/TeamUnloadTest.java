package com.martelstudios.openquests.core.scopes;

import com.martelstudios.openquests.core.QuestHarness;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.teams.TestTeamTypes;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamUnloadTest {

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
