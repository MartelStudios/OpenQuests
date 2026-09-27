package com.martelstudios.openquests.extension.quests.block;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.martelstudios.openquests.core.persistence.CodecJson;
import com.martelstudios.openquests.extension.quests.placeblock.PlaceBlockQuestProgression;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlacedBlockMarksTest {

    @Test
    void aBrokenBlockIsOnlyAPlayersOnce() {
        PlacedBlockMarks marks = new PlacedBlockMarks();
        marks.mark(ChunkUtil.indexBlock(3, 70, 12));

        assertTrue(marks.unmark(ChunkUtil.indexBlock(3, 70, 12)));
        assertFalse(marks.unmark(ChunkUtil.indexBlock(3, 70, 12)));
        assertTrue(marks.isEmpty());
    }

    @Test
    void aFewMarksAreSavedAsTheirPositions() {
        PlacedBlockMarks marks = new PlacedBlockMarks();
        marks.mark(0);
        marks.mark(1234);
        marks.mark(ChunkUtil.SIZE_BLOCKS - 1);

        BsonDocument saved = PlacedBlockMarks.CODEC.encode(marks, ExtraInfo.THREAD_LOCAL.get());
        PlacedBlockMarks loaded = PlacedBlockMarks.CODEC.decode(saved, ExtraInfo.THREAD_LOCAL.get());

        assertNotNull(loaded);
        assertTrue(loaded.isMarked(0));
        assertTrue(loaded.isMarked(1234));
        assertTrue(loaded.isMarked(ChunkUtil.SIZE_BLOCKS - 1));
        assertFalse(loaded.isMarked(1235));
        assertEquals(1 + 3 * Short.BYTES, saved.getBinary("Data").getData().length);
    }

    @Test
    void aBusySectionIsSavedAsBitsAndSmaller() {
        PlacedBlockMarks marks = new PlacedBlockMarks();
        for (int index = 0; index < 20_000; index++) marks.mark(index);

        BsonDocument saved = PlacedBlockMarks.CODEC.encode(marks, ExtraInfo.THREAD_LOCAL.get());
        PlacedBlockMarks loaded = PlacedBlockMarks.CODEC.decode(saved, ExtraInfo.THREAD_LOCAL.get());

        assertNotNull(loaded);
        assertTrue(loaded.isMarked(19_999));
        assertFalse(loaded.isMarked(20_000));
        assertTrue(saved.getBinary("Data").getData().length <= ChunkUtil.SIZE_BLOCKS / 8 + 1, "the bits should beat the list of positions");
    }

    @Test
    void placingAgainWhatABreakGaveBackDoesNotCount() {
        UUID player = UUID.randomUUID();
        BlockRecoveries recoveries = new BlockRecoveries();

        assertFalse(recoveries.consume(player));
        recoveries.record(player);

        assertTrue(recoveries.consume(player));
        assertFalse(recoveries.consume(player));
    }

    @Test
    void recoveriesOutliveTheSessionTheyWereMadeIn() {
        UUID player = UUID.randomUUID();
        PlaceBlockQuestProgression quest = new PlaceBlockQuestProgression();
        quest.recoveries.record(player);
        quest.recoveries.record(player);

        String saved = CodecJson.encode(PlaceBlockQuestProgression.CODEC, quest);
        PlaceBlockQuestProgression loaded = CodecJson.decode(PlaceBlockQuestProgression.CODEC, saved, "quest");

        assertNotNull(loaded);
        assertEquals(2, loaded.recoveries.recovered.get(player.toString()));
    }
}
