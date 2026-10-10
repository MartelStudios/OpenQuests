package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.MovementStates;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.sync.QuestSync;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementReplayTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @AfterEach
    void alone() {
        QuestSync.setPolicy(shared(false));
    }

    @Test
    void aSampleCountsOnAStoredCopyWhatItCountedHere() {
        QuestSync.setPolicy(shared(true));

        PaceQuest here = new PaceQuest();
        here.join(PLAYER);
        here.setTargetQuantity(10);
        // Written as it stands, so what follows is kept operation by operation
        here.markStored(new AbstractQuestProgression.Stored(1, QuestState.IN_PROGRESS, Set.of(PLAYER), Set.of()));
        here.consumeChanges();

        new MovementQuestVisitor(PLAYER, new MovementStates(), 0.6, 0.05).progress(here);
        PaceQuest stored = CodecJson.decode(PaceQuest.CODEC, CodecJson.encode(PaceQuest.CODEC, here), "quest");

        // The second sample makes a whole metre here, from the fraction only this copy carries
        new MovementQuestVisitor(PLAYER, new MovementStates(), 0.6, 0.05).progress(here);
        for (QuestOperation<?> operation : here.getPendingOperations()) stored.replay(operation);

        assertEquals(1, here.getCurrentQuantity());
        assertEquals(1, stored.getCurrentQuantity());
    }

    @Test
    void countsKeptBetweenTwoWritesGoAsOne() {
        QuestSync.setPolicy(shared(true));

        PaceQuest quest = new PaceQuest();
        quest.setTargetQuantity(10);
        quest.markStored(new AbstractQuestProgression.Stored(1, QuestState.IN_PROGRESS, Set.of(), Set.of()));

        quest.apply(new QuantityQuestProgression.Add(1));
        quest.apply(new QuantityQuestProgression.Add(2));

        assertEquals(List.of(new QuantityQuestProgression.Add(3)), quest.getPendingOperations());
    }

    @Test
    void aCountMadeAgainOnACopyEndedElsewhereCountsNothing() {
        PaceQuest ended = new PaceQuest();
        ended.setTargetQuantity(2);

        assertTrue(ended.replay(new QuantityQuestProgression.Add(2)));
        assertEquals(QuestState.SUCCESSFUL, ended.getState());

        assertFalse(ended.replay(new QuantityQuestProgression.Add(1)));
        assertEquals(2, ended.getCurrentQuantity());
    }

    /**
     * @param stored whether a quest written once is shared, as on a database several servers write
     */
    @Nonnull
    private static QuestSync.Policy shared(boolean stored) {
        return new QuestSync.Policy() {
            @Override
            public boolean isShared(@Nonnull AbstractQuestProgression<?> quest) {
                return stored && quest.getStoredVersion() > 0;
            }

            @Override
            public void writeSoon(@Nonnull AbstractQuestProgression<?> quest) {}
        };
    }

    /**
     * Counts metres, standing in for the shipped paces without an asset store.
     */
    static final class PaceQuest extends MovementQuestProgression<PaceQuest> {
        static final BuilderCodec<PaceQuest> CODEC = BuilderCodec.builder(PaceQuest.class, PaceQuest::new, QuantityQuestProgression.BASE_CODEC).build();

        void join(@Nonnull UUID playerId) {
            apply(new QuestOperation.Join(playerId));
        }

        @Override
        protected double advance(@Nonnull MovementStates states, double metres, double seconds) {
            return metres;
        }

        @Override
        public boolean isStopOnComplete() {
            return true;
        }
    }
}
