package com.martelstudios.openquests.core;

import com.martelstudios.openquests.core.constraints.AssignmentRequest;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.events.QuestUnregisteredEvent;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.PlayerMessage;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import com.martelstudios.openquests.core.persistence.TestQuestReward;
import com.martelstudios.openquests.core.rewards.services.QuestRewardService;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.visitors.PlayerSetStateVisitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the services do with a quest a player holds: their record, its end, what it pays them
 * whether they are there or not, and giving it up. As they do it today.
 */
class QuestPlayerFlowTest {

    private final QuestHarness harness = new QuestHarness();
    private final QuestProgressionService progression = harness.services().getQuestProgressionService();
    private final QuestRewardService rewards = harness.services().getQuestRewardService();

    @AfterEach
    void close() {
        harness.close();
    }

    @Test
    void aQuestHandedToAPlayerIsListedInTheirRecord() {
        UUID player = harness.players().connect();
        harness.hear(QuestAddedToPlayerStoreEvent.class);

        TestQuestProgression quest = harness.assign(new TestQuestAsset(), player);

        assertNotNull(quest);
        assertEquals(Set.of(player), quest.getPlayers());
        assertEquals(Set.of(quest.getId()), harness.questsOf(player));
        assertEquals(List.of(QuestAddedToPlayerStoreEvent.class), harness.heard());
    }

    @Test
    void aPlayerTheConstraintsRefuseIsHandedNothing() {
        UUID player = harness.players().connect();

        TestQuestProgression quest = harness.assign(new TestQuestAsset(new Refusing()), player);

        assertNull(quest);
        assertTrue(progression.getAllQuests().isEmpty(), "nothing is left registered that nobody holds");
        assertTrue(harness.questsOf(player).isEmpty());
    }

    @Test
    void anEndAPlayerHoldsIsKeptAsHistory() {
        UUID player = harness.players().connect();
        TestQuestProgression quest = harness.assign(new TestQuestAsset(), player);
        harness.hear(QuestUnregisteredEvent.class);

        harness.count(quest, 5, 5);

        assertEquals(QuestState.SUCCESSFUL, quest.getState());
        assertNull(progression.getLiveQuest(quest.getId()));
        assertSame(quest, progression.getQuest(quest.getId()));
        assertEquals(List.of(), harness.heard(), "a quest someone holds is archived, not unregistered");
        assertEquals(Set.of(quest.getId()), harness.questsOf(player), "and stays in their record for the journal");
    }

    @Test
    void anOnlinePlayerIsOwedWhatTheEndPays() {
        UUID player = harness.players().connect();
        TestQuestProgression quest = harness.assign(new TestQuestAsset().paying(new TestQuestReward("gold")), player);

        harness.count(quest, 5, 5);

        assertTrue(rewards.isOwed(quest.getId(), harness.players().of(player)));
        assertTrue(harness.storage().loadMessages(Set.of(player)).isEmpty());
    }

    @Test
    void anOfflinePlayerIsLeftTheDebtAsAMessage() {
        UUID player = UUID.randomUUID();
        TestQuestProgression quest = harness.assign(new TestQuestAsset().paying(new TestQuestReward("gold")), player);

        harness.count(quest, 5, 5);

        assertFalse(rewards.isOwed(quest.getId(), harness.players().of(player)), "nothing is written into an absent player");
        List<PlayerMessage> messages = harness.storage().loadMessages(Set.of(player));
        assertEquals(1, messages.size());
        assertNotNull(messages.getFirst().getOwed());
        assertEquals(quest.getId(), messages.getFirst().getOwed().getQuestId());
    }

    @Test
    void theLastHolderGivingUpAbandonsTheQuestAndIsPaidForIt() {
        UUID player = harness.players().connect();
        TestQuestProgression quest = harness.assign(new TestQuestAsset().payingOnAbandon(new TestQuestReward("consolation")), player);
        harness.hear(QuestRemovedFromPlayerStoreEvent.class);

        progression.progress(new PlayerSetStateVisitor(player, quest.getId().toString(), QuestState.ABANDONED), List.of(quest.getId()));

        assertEquals(QuestState.ABANDONED, quest.getState());
        assertEquals(Set.of(player), quest.getAbandonedPlayers());
        assertTrue(rewards.isOwed(quest.getId(), harness.players().of(player)));
    }

    /**
     * Turns every player away.
     */
    private static final class Refusing extends QuestConstraint {
        @Override
        public boolean allowsAssignment(@Nonnull AssignmentRequest request) {
            return false;
        }
    }
}
