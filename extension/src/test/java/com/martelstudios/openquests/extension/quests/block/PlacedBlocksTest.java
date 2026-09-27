package com.martelstudios.openquests.extension.quests.block;

import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.extension.quests.breakblock.BreakBlockQuestProgression;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedBlocksTest {

    private static final String HERE = PlacedBlocks.position("default", 10, 64, -3);
    private static final String THERE = PlacedBlocks.position("default", 11, 64, -3);

    @Test
    void aPlaceAndBreakLoopOnlyCountsTheFirstPlacement() {
        UUID player = UUID.randomUUID();
        PlacedBlocks placed = new PlacedBlocks();

        assertTrue(placed.recordPlacement(player, HERE));
        assertTrue(placed.recordBreak(player, HERE));
        assertFalse(placed.recordPlacement(player, HERE));
        assertTrue(placed.recordBreak(player, HERE));
    }

    @Test
    void aBlockNobodyPlacedIsTheWorldsOwn() {
        PlacedBlocks placed = new PlacedBlocks();

        assertFalse(placed.recordBreak(UUID.randomUUID(), HERE));
    }

    @Test
    void oneWorldsBlocksAreNotAnothersSamePlace() {
        PlacedBlocks placed = new PlacedBlocks();
        placed.recordPlacement(UUID.randomUUID(), HERE);

        assertFalse(placed.recordBreak(UUID.randomUUID(), PlacedBlocks.position("instance-Forgotten_Temple-1", 10, 64, -3)));
    }

    @Test
    void aBlockPlacedForAnotherPlayerToBreakIsTheSameLoop() {
        UUID placer = UUID.randomUUID();
        UUID breaker = UUID.randomUUID();
        PlacedBlocks placed = new PlacedBlocks();

        placed.recordPlacement(placer, HERE);

        assertTrue(placed.recordBreak(breaker, HERE));
        assertFalse(placed.recordPlacement(breaker, THERE));
        assertTrue(placed.recordPlacement(placer, THERE));
    }

    @Test
    void placedBlocksOutliveTheSessionTheyWerePlacedIn() {
        UUID player = UUID.randomUUID();
        BreakBlockQuestProgression quest = new BreakBlockQuestProgression();
        quest.placed.recordPlacement(player, HERE);

        String saved = CodecJson.encode(BreakBlockQuestProgression.CODEC, quest);
        BreakBlockQuestProgression loaded = CodecJson.decode(BreakBlockQuestProgression.CODEC, saved, "quest");

        assertNotNull(loaded);
        assertTrue(loaded.placed.recordBreak(player, HERE));
    }
}
