package com.martelstudios.openquests.extension.quests.item;

import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.extension.quests.pickupitem.PickupItemQuestProgression;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExchangeCountersTest {

    @Test
    void anExchangeCountsTheItemsItMoves() {
        PickupItemQuestProgression quest = new PickupItemQuestProgression() {
            @Override
            public boolean isAntiAbuse() {
                return false;
            }

            @Override
            public boolean isStopOnComplete() {
                return true;
            }
        };
        quest.setTargetQuantity(10);

        assertTrue(quest.replay(new ItemExchangeQuestProgression.Exchange(UUID.randomUUID(), ItemExchange.GROUND_PICKUP, 3)));
        assertEquals(3, quest.getCurrentQuantity());
    }

    @Test
    void aThrowAndPickupLoopOnlyCountsTheFirstThrow() {
        UUID player = UUID.randomUUID();
        ExchangeCounters counters = new ExchangeCounters();

        assertEquals(5, counters.recordThrow(player, 5));
        assertEquals(0, counters.recordGroundPickup(player, 5));
        assertEquals(0, counters.recordThrow(player, 5));
        assertEquals(0, counters.recordGroundPickup(player, 5));
    }

    @Test
    void itemsNeverThrownCountBothWays() {
        UUID player = UUID.randomUUID();
        ExchangeCounters counters = new ExchangeCounters();

        assertEquals(5, counters.recordGroundPickup(player, 5));
        assertEquals(5, counters.recordThrow(player, 5));
    }

    @Test
    void onlyThePartOnceThrownIsLeftOut() {
        UUID player = UUID.randomUUID();
        ExchangeCounters counters = new ExchangeCounters();

        counters.recordThrow(player, 2);

        assertEquals(3, counters.recordGroundPickup(player, 5));
        assertEquals(1, counters.recordThrow(player, 3));
    }

    @Test
    void eachPlayerKeepsTheirOwnCount() {
        ExchangeCounters counters = new ExchangeCounters();

        counters.recordThrow(UUID.randomUUID(), 5);

        assertEquals(5, counters.recordGroundPickup(UUID.randomUUID(), 5));
    }

    @Test
    void aThrowOutlivesTheSessionItWasMadeIn() {
        UUID player = UUID.randomUUID();
        PickupItemQuestProgression quest = new PickupItemQuestProgression();
        quest.counters.recordThrow(player, 5);

        String saved = CodecJson.encode(PickupItemQuestProgression.CODEC, quest);
        PickupItemQuestProgression loaded = CodecJson.decode(PickupItemQuestProgression.CODEC, saved, "quest");

        assertNotNull(loaded);
        assertEquals(0, loaded.counters.recordGroundPickup(player, 5));
    }

    @Test
    void spentCountsAreLeftOutOfTheSave() {
        UUID player = UUID.randomUUID();
        PickupItemQuestProgression quest = new PickupItemQuestProgression();
        quest.counters.recordThrow(player, 5);
        quest.counters.recordGroundPickup(player, 5);
        quest.counters.recordThrow(player, 5);
        quest.counters.recordGroundPickup(player, 5);
        quest.counters.recordThrow(player, 5);

        assertFalse(CodecJson.encode(PickupItemQuestProgression.CODEC, quest).contains("Recovered"));
    }
}
