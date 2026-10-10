package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.MovementStates;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementReplayTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void aSampleCountsOnAStoredCopyWhatItCountedHere() {
        PaceQuest here = new PaceQuest();
        here.join(PLAYER);
        here.setTargetQuantity(10);

        new MovementQuestVisitor(PLAYER, new MovementStates(), 0.6, 0.05).progress(here);
        PaceQuest stored = CodecJson.decode(PaceQuest.CODEC, CodecJson.encode(PaceQuest.CODEC, here), "quest");

        // The second sample makes a whole metre here, from the fraction only this copy carries
        MovementQuestVisitor second = new MovementQuestVisitor(PLAYER, new MovementStates(), 0.6, 0.05);
        second.progress(here);
        second.progress(stored);

        assertEquals(1, here.getCurrentQuantity());
        assertEquals(1, stored.getCurrentQuantity());
    }

    /**
     * Counts metres, standing in for the shipped paces without an asset store.
     */
    static final class PaceQuest extends MovementQuestProgression<PaceQuest> {
        static final BuilderCodec<PaceQuest> CODEC = BuilderCodec.builder(PaceQuest.class, PaceQuest::new, QuantityQuestProgression.BASE_CODEC).build();

        void join(@Nonnull UUID playerId) {
            change(quest -> quest.players.add(playerId));
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
