package com.martelstudios.opendialogues.characters.runtime;

import com.martelstudios.opendialogues.CodecJson;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReleasedNpcsTest {

    private static final Instant NOW = Instant.parse("2026-11-22T12:00:00Z");

    @Test
    void prunedEntriesAreForgotten() {
        ReleasedNpcs released = new ReleasedNpcs();
        UUID old = UUID.randomUUID();
        UUID recent = UUID.randomUUID();
        released.add(old, NOW.minusSeconds(100));
        released.add(recent, NOW);

        released.pruneBefore(NOW.minusSeconds(10));

        assertNull(released.releasedAt(old));
        assertEquals(NOW, released.releasedAt(recent));
    }

    @Test
    void survivesACodecRoundTrip() {
        ReleasedNpcs released = new ReleasedNpcs();
        UUID npc = UUID.randomUUID();
        released.add(npc, NOW);

        ReleasedNpcs read = CodecJson.roundTrip(ReleasedNpcs.CODEC, released);

        assertEquals(NOW, read.releasedAt(npc));
        assertNull(read.releasedAt(UUID.randomUUID()));
        read.pruneBefore(NOW.plusSeconds(1));
        assertNull(read.releasedAt(npc));
    }

    @Test
    void releasedSinceCountsOnlyAReleaseAfterTheTie() {
        assertTrue(ReleasedNpcs.releasedSince(NOW, NOW.minusSeconds(1)));
        assertTrue(ReleasedNpcs.releasedSince(NOW, NOW));
        assertTrue(ReleasedNpcs.releasedSince(NOW, null));
        assertFalse(ReleasedNpcs.releasedSince(NOW, NOW.plusSeconds(1)));
        assertFalse(ReleasedNpcs.releasedSince(null, NOW));
    }
}
