package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.event.EventBus;
import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.protocol.MovementStates;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestEnvironment;
import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestReplica;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementReplayTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void aSampleCountsOnAStoredCopyWhatItCountedHere() {
        PaceQuest here = new PaceQuest.Builder().targetQuantity(10).build();
        here.attach(ON_SHARED_STORAGE);
        here.join(PLAYER);
        // Written as it stands, so what follows is kept operation by operation
        here.getReplica().markStored(new QuestReplica.Stored(1, QuestState.IN_PROGRESS, Set.of(PLAYER), Set.of()));
        here.consumeChanges();

        new MovementQuestVisitor(PLAYER, new MovementStates(), 0.6, 0.05).progress(here);
        PaceQuest stored = CodecJson.decode(PaceQuest.CODEC, CodecJson.encode(PaceQuest.CODEC, here), "quest");

        // The second sample makes a whole metre here, from the fraction only this copy carries
        new MovementQuestVisitor(PLAYER, new MovementStates(), 0.6, 0.05).progress(here);
        for (QuestOperation<?> operation : here.getReplica().getPendingOperations()) stored.getReplica().replay(operation);

        assertEquals(1, here.getCurrentQuantity());
        assertEquals(1, stored.getCurrentQuantity());
    }

    @Test
    void countsKeptBetweenTwoWritesGoAsOne() {
        PaceQuest quest = new PaceQuest.Builder().targetQuantity(10).build();
        quest.attach(ON_SHARED_STORAGE);
        quest.getReplica().markStored(new QuestReplica.Stored(1, QuestState.IN_PROGRESS, Set.of(), Set.of()));

        quest.apply(new QuantityQuestProgression.Add(1));
        quest.apply(new QuantityQuestProgression.Add(2));

        assertEquals(List.of(new QuantityQuestProgression.Add(3)), quest.getReplica().getPendingOperations());
    }

    @Test
    void aCountMadeAgainOnACopyEndedElsewhereCountsNothing() {
        PaceQuest ended = new PaceQuest.Builder().targetQuantity(2).build();

        assertTrue(ended.getReplica().replay(new QuantityQuestProgression.Add(2)));
        assertEquals(QuestState.SUCCESSFUL, ended.getState());

        assertFalse(ended.getReplica().replay(new QuantityQuestProgression.Add(1)));
        assertEquals(2, ended.getCurrentQuantity());
    }

    /**
     * A quest written once is shared, as on a database several servers write.
     */
    private static final QuestEnvironment ON_SHARED_STORAGE = new QuestEnvironment() {
        private final EventBus events = new EventBus(false);

        @Override
        public boolean isShared(@Nonnull AbstractQuestProgression<?> quest) {
            return quest.getReplica().getVersion() > 0;
        }

        @Override
        public void writeSoon(@Nonnull AbstractQuestProgression<?> quest) {}

        @Nonnull
        @Override
        public IEventBus events() {
            return events;
        }

        @Nonnull
        @Override
        public Clock clock() {
            return Clock.systemUTC();
        }

        @Nonnull
        @Override
        public OpenQuestsServices services() {
            throw new UnsupportedOperationException("a replay test runs no services");
        }
    };

    /**
     * Counts metres, standing in for the shipped paces without an asset store.
     */
    static final class PaceQuest extends MovementQuestProgression<PaceQuest> {
        static final BuilderCodec<PaceQuest> CODEC = BuilderCodec.builder(PaceQuest.class, PaceQuest::new, QuantityQuestProgression.BASE_CODEC).build();

        static final class Builder extends QuantityQuestProgression.Builder<PaceQuest, Builder> {
            Builder() {
                super(new PaceQuest(), "Pace");
            }
        }

        void join(@Nonnull UUID playerId) {
            apply(new AbstractQuestProgression.Join(playerId));
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
