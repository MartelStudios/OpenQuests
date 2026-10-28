package com.martelstudios.opendialogues.conditions;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.opendialogues.history.DialogueHistory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConditionContextTest {

    private final PlayerRef player = new PlayerRef(null, UUID.randomUUID(), "Tester", "en-US", null, null);

    @Test
    void aPlayerKeepsTheHistoryRead() {
        DialogueHistory history = new DialogueHistory();

        assertSame(history, ConditionContext.of(player, history).history());
    }

    @Test
    void anUnreadableHistoryIsJudgedEmpty() {
        assertNotNull(ConditionContext.of(player, null).history());
    }

    @Test
    void aPlayerBetweenWorldsHasNoEntityToRead() {
        ConditionContext context = ConditionContext.of(player, null);

        assertNull(context.entity());
        assertNull(context.store());
        assertFalse(context.canReadEntity());
    }
}
