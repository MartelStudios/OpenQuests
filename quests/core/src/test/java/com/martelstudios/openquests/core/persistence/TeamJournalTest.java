package com.martelstudios.openquests.core.persistence;

import com.martelstudios.openquests.core.persistence.disk.TeamJournal;
import com.martelstudios.openquests.core.teams.TeamChange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TeamJournalTest {
    @TempDir Path folder;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void aRestartCompletesAnInterruptedTeamWriteBeforeNewWork(boolean withMessage) {
        UUID player = UUID.randomUUID();
        TeamRecord team = new TeamRecord(UUID.randomUUID(), "party", null, "Alice", player, List.of(player), List.of());
        TeamTransaction transaction = new TeamTransaction(List.of(team), withMessage ? PlayerMessage.team(player, new TeamChange("party", null, team.getId())) : null)
                .addToIndex("team-invitations:" + player, team.getId());
        TeamJournal interrupted = new TeamJournal(folder, value -> { throw new IllegalStateException("disk failure after intent"); });
        assertThrows(IllegalStateException.class, () -> interrupted.commit(transaction));

        AtomicInteger replays = new AtomicInteger();
        TeamJournal restarted = new TeamJournal(folder, recovered -> {
            assertEquals(team.getId(), recovered.teams().getFirst().getId());
            assertEquals(transaction.id(), recovered.id());
            assertEquals(transaction.added(), recovered.added());
            if (withMessage) {
                assertEquals(transaction.message().getId(), recovered.message().getId());
                assertTrue(recovered.message().getTeam().revision() > 0);
            } else assertNull(recovered.message());
            replays.incrementAndGet();
        });
        restarted.recover();
        restarted.recover();
        assertEquals(1, replays.get());
    }
}
