package com.martelstudios.openquests.extension.quests.quantity;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.replication.Replica;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantityMergeTest {

    @AfterEach
    void standAloneAgain() {
        Replica.setLocalId(Replica.DEFAULT_ID);
    }

    @Test
    void twoServersCountingOneQuestAddUp() {
        CountingQuest onA = new CountingQuest();
        CountingQuest onB = copyOf(onA);

        Replica.setLocalId("a");
        onA.setCurrentQuantity(onA.getCurrentQuantity() + 4);
        Replica.setLocalId("b");
        onB.setCurrentQuantity(onB.getCurrentQuantity() + 3);

        assertTrue(onA.merge(onB));
        assertEquals(7, onA.getCurrentQuantity());
    }

    @Test
    void aCountReadFromAnOlderSaveIsKept() {
        CountingQuest read = CodecJson.decode(CountingQuest.CODEC, "{\"CurrentQuantity\": 12}", "quest");

        assertEquals(12, read.getCurrentQuantity());
    }

    private static CountingQuest copyOf(CountingQuest quest) {
        return CodecJson.decode(CountingQuest.CODEC, CodecJson.encode(CountingQuest.CODEC, quest), "quest");
    }

    /**
     * The smallest counted quest, standing in for the shipped types without an asset store.
     */
    static final class CountingQuest extends QuantityQuestProgression<CountingQuest> {
        static final BuilderCodec<CountingQuest> CODEC = BuilderCodec.builder(CountingQuest.class, CountingQuest::new, QuantityQuestProgression.BASE_CODEC).build();
    }
}
