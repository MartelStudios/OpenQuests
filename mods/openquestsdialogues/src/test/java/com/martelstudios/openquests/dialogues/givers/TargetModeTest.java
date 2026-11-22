package com.martelstudios.openquests.dialogues.givers;

import com.martelstudios.openquests.core.targets.QuestTarget;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetModeTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    private static TestQuest givenBy(UUID npc) {
        return TestQuest.givenBy(new TestQuest(), new CharacterGiver("Farmer", npc, null));
    }

    @Test
    void giverNpcAcceptsOnlyThatNpc() {
        QuestTarget target = TargetMode.GiverNpc.resolve(null, givenBy(A));

        assertEquals(new QuestTarget.Npc(A, "Farmer"), target);
        assertTrue(TargetMode.accepts(target, "Farmer", A));
        assertFalse(TargetMode.accepts(target, "Farmer", B));
    }

    @Test
    void giverCharacterAcceptsAnyNpcOfIt() {
        QuestTarget target = TargetMode.GiverCharacter.resolve("Ignored", givenBy(A));

        assertEquals(new QuestTarget.Character("Farmer"), target);
        assertTrue(TargetMode.accepts(target, "Farmer", B));
        assertFalse(TargetMode.accepts(target, "Mage", A));
    }

    @Test
    void characterModeKeepsTheFixedCharacter() {
        assertEquals(new QuestTarget.Character("Mage"), TargetMode.Character.resolve("Mage", givenBy(A)));
        assertNull(TargetMode.Character.resolve(null, givenBy(A)));
    }

    @Test
    void noGiverResolvesToNoTarget() {
        assertNull(TargetMode.GiverNpc.resolve("Farmer", new TestQuest()));
        assertNull(TargetMode.GiverCharacter.resolve("Farmer", new TestQuest()));
        assertFalse(TargetMode.accepts(null, "Farmer", A));
    }
}
