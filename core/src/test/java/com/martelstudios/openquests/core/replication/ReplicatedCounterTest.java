package com.martelstudios.openquests.core.replication;

import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplicatedCounterTest {

    @AfterEach
    void standAloneAgain() {
        Replica.setLocalId(Replica.DEFAULT_ID);
    }

    @Test
    void twoServersCountingAtOnceLoseNothing() {
        ReplicatedCounter onA = on("a", 5);
        ReplicatedCounter onB = on("b", 3);

        assertTrue(onA.merge(onB));
        assertTrue(onB.merge(onA));

        assertEquals(8, onA.get());
        assertEquals(8, onB.get());
    }

    @Test
    void mergingTheSameCopyAgainChangesNothing() {
        ReplicatedCounter onA = on("a", 5);
        ReplicatedCounter onB = on("b", 3);
        onA.merge(onB);

        assertFalse(onA.merge(onB));
        assertEquals(8, onA.get());
    }

    @Test
    void settingTheWholeMovesOnlyThisServersShare() {
        ReplicatedCounter onA = on("a", 5);
        ReplicatedCounter onB = on("b", 3);
        onB.merge(onA);

        Replica.setLocalId("b");
        onB.set(6);
        onA.merge(onB);

        assertEquals(6, onA.get());
    }

    @Test
    void aCountGoingDownStaysMergeable() {
        ReplicatedCounter onA = on("a", 5);
        onA.add(-2);

        ReplicatedCounter onB = on("b", 1);
        onB.merge(onA);

        assertEquals(4, onB.get());
    }

    @Test
    void aCountWrittenBeforeSharingIsCountedOnce() {
        ReplicatedCounter readOnA = new ReplicatedCounter();
        readOnA.restore("", 7);
        ReplicatedCounter readOnB = new ReplicatedCounter();
        readOnB.restore("", 7);

        readOnA.merge(readOnB);

        assertEquals(7, readOnA.get());
    }

    @Test
    void theSlotsSurviveTheCodec() {
        ReplicatedCounter onA = on("a", 5);
        onA.merge(on("b", 3));

        ReplicatedCounter read = CodecJson.decode(ReplicatedCounter.CODEC, CodecJson.encode(ReplicatedCounter.CODEC, onA), "counter");

        assertEquals(8, read.get());
    }

    private static ReplicatedCounter on(String server, long added) {
        Replica.setLocalId(server);
        ReplicatedCounter counter = new ReplicatedCounter();
        counter.add(added);
        return counter;
    }
}
