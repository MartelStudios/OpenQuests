package com.martelstudios.openquests.extension.quests.loot.npc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

// The group needs the asset store, absent from unit tests, so the numbers are checked on their own
class LootNpcDropTest {

    @Test
    void defaultsApply() {
        LootNpcDrop drop = new LootNpcDrop();

        assertEquals(1.0, drop.getChance());
        assertEquals(1, drop.getMin());
        assertEquals(1, drop.getMax());
        assertNull(drop.getGuaranteedAfter());
        assertNull(drop.problem());
    }

    @Test
    void rejectsChanceOutOfRange() {
        LootNpcDrop drop = new LootNpcDrop();
        drop.chance = 0;
        assertNotNull(drop.problem());

        drop.chance = 1.5;
        assertNotNull(drop.problem());
    }

    @Test
    void rejectsMinAboveMax() {
        LootNpcDrop drop = new LootNpcDrop();
        drop.min = 3;
        drop.max = 2;

        assertNotNull(drop.problem());
    }

    @Test
    void rejectsMinBelowOne() {
        LootNpcDrop drop = new LootNpcDrop();
        drop.min = 0;

        assertNotNull(drop.problem());
    }

    @Test
    void rejectsGuaranteeBelowOne() {
        LootNpcDrop drop = new LootNpcDrop();
        drop.guaranteedAfter = 0;

        assertNotNull(drop.problem());
    }
}
