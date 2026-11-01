package com.martelstudios.openquests.dialogues.quests.dialogue;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DialogueQuestDeathNoticesTest {

    @Test
    void holders_outside_the_npc_world_are_told() {
        UUID p1 = UUID.randomUUID(), p2 = UUID.randomUUID(), p3 = UUID.randomUUID(), p4 = UUID.randomUUID(), p5 = UUID.randomUUID();
        List<DialogueQuestDeathNotices.Holding> quests = List.of(
            new DialogueQuestDeathNotices.Holding("Farmer", Set.of(p1, p2), false),
            new DialogueQuestDeathNotices.Holding("Farmer", Set.of(p3), true),
            new DialogueQuestDeathNotices.Holding("Mage", Set.of(p4), false),
            new DialogueQuestDeathNotices.Holding(null, Set.of(p5), false));

        assertEquals(Set.of(p1), DialogueQuestDeathNotices.recipients(quests, "Farmer", Set.of(p2)));
    }

    @Test
    void a_player_holding_two_quests_is_told_once() {
        UUID p1 = UUID.randomUUID();
        List<DialogueQuestDeathNotices.Holding> quests = List.of(
            new DialogueQuestDeathNotices.Holding("Farmer", Set.of(p1), false),
            new DialogueQuestDeathNotices.Holding("Farmer", Set.of(p1), false));

        assertEquals(Set.of(p1), DialogueQuestDeathNotices.recipients(quests, "Farmer", Set.of()));
    }
}
