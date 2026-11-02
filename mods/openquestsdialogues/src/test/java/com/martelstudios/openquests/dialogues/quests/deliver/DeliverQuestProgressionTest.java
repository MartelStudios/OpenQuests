package com.martelstudios.openquests.dialogues.quests.deliver;

import com.hypixel.hytale.codec.ExtraInfo;
import org.bson.BsonValue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeliverQuestProgressionTest {

    @Test
    void deliveries_add_up_per_item_and_cap() {
        int[] delivered = new int[2];
        int[] quantities = {10, 5};

        assertEquals(4, DeliverQuestProgression.accept(delivered, quantities, 0, 4));
        assertEquals(6, DeliverQuestProgression.accept(delivered, quantities, 0, 9));
        assertEquals(0, DeliverQuestProgression.accept(delivered, quantities, 0, 3));
        assertEquals(0, DeliverQuestProgression.accept(delivered, quantities, 2, 3));
        assertEquals(5, DeliverQuestProgression.accept(delivered, quantities, 1, 5));
        assertArrayEquals(new int[]{10, 5}, delivered);
    }

    @Test
    void delivered_survives_a_codec_round_trip() {
        DeliverQuestProgression quest = new DeliverQuestProgression();
        quest.delivered = new int[]{4, 2};

        ExtraInfo extraInfo = ExtraInfo.THREAD_LOCAL.get();
        BsonValue encoded = DeliverQuestProgression.CODEC.encode(quest, extraInfo);
        DeliverQuestProgression read = DeliverQuestProgression.CODEC.decode(encoded, extraInfo);

        assertEquals(4, read.delivered(0));
        assertEquals(2, read.delivered(1));
        assertEquals(0, read.delivered(5));
    }
}
