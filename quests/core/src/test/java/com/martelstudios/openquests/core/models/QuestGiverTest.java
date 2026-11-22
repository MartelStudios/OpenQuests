package com.martelstudios.openquests.core.models;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.TestQuestAsset;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.core.persistence.TestQuestProgression;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestGiverTest {

    static {
        TestGiver.register();
    }

    @Test
    void roundTripKeepsEveryField() {
        TestQuestProgression quest = withGiver(new TestGiver(UUID.randomUUID(), "farmers.mira", Instant.ofEpochMilli(1_700_000_000_000L), "kept"));

        TestQuestProgression read = CodecJson.decode(TestQuestProgression.CODEC, CodecJson.encode(TestQuestProgression.CODEC, quest), "quest");

        assertEquals(quest.getGiver(), read.getGiver());
    }

    @Test
    void unknownTypeLoadsWithoutGiver() {
        TestQuestProgression quest = withGiver(new TestGiver(UUID.randomUUID(), "farmers.mira", null, "kept"));
        String json = CodecJson.encode(TestQuestProgression.CODEC, quest);
        assertTrue(json.contains("\"" + TestGiver.TYPE + "\""));

        TestQuestProgression read = CodecJson.decode(TestQuestProgression.CODEC, json.replace("\"" + TestGiver.TYPE + "\"", "\"Gone\""), "quest");

        assertNull(read.getGiver());
        assertEquals(quest.getId(), read.getId());
    }

    @Test
    void noGiverWritesNoKey() {
        TestQuestProgression quest = (TestQuestProgression) new TestQuestAsset().create();

        assertFalse(CodecJson.encode(TestQuestProgression.CODEC, quest).contains("\"Giver\""));
    }

    @Test
    void equalByValue() {
        UUID npc = UUID.randomUUID();

        assertEquals(new TestGiver(npc, "a", null, "x"), new TestGiver(npc, "a", null, "x"));
        assertEquals(new TestGiver(npc, "a", null, "x").hashCode(), new TestGiver(npc, "a", null, "x").hashCode());
        assertNotEquals(new TestGiver(npc, "a", null, "x"), new TestGiver(npc, "b", null, "x"));
        assertNotEquals(new TestGiver(npc, "a", null, "x"), new TestGiver(npc, "a", null, "y"));
    }

    static TestQuestProgression withGiver(QuestGiver giver) {
        return (TestQuestProgression) new TestQuestAsset().builder().giver(giver).build();
    }

    /**
     * A giver kind of the tests' own, with a field of its own to prove subtypes extend the base.
     */
    static final class TestGiver extends QuestGiver {

        static final String TYPE = "CoreTestGiver";

        static final BuilderCodec<TestGiver> CODEC = BuilderCodec.builder(TestGiver.class, TestGiver::new, QuestGiver.BASE_CODEC)
                                                                 .append(new KeyedCodec<>("Extra", Codec.STRING), (giver, extra) -> giver.extra = extra, giver -> giver.extra)
                                                                 .add()
                                                                 .build();

        private static boolean registered;

        private String extra;

        private TestGiver() {}

        TestGiver(UUID npc, String nameKey, Instant since, String extra) {
            super(npc, nameKey, since);
            this.extra = extra;
        }

        static synchronized void register() {
            if (registered) return;

            QuestGiver.CODEC.register(TYPE, TestGiver.class, CODEC);
            registered = true;
        }

        @Override
        public boolean equals(Object other) {
            return super.equals(other) && Objects.equals(extra, ((TestGiver) other).extra);
        }

        @Override
        public int hashCode() {
            return 31 * super.hashCode() + Objects.hashCode(extra);
        }
    }
}
