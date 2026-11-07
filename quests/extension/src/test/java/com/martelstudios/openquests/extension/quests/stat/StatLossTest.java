package com.martelstudios.openquests.extension.quests.stat;

import com.hypixel.hytale.protocol.EntityStatOp;
import com.hypixel.hytale.protocol.EntityStatUpdate;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.hypixel.hytale.protocol.EntityStatOp.Add;
import static com.hypixel.hytale.protocol.EntityStatOp.Init;
import static com.hypixel.hytale.protocol.EntityStatOp.Min;
import static com.hypixel.hytale.protocol.EntityStatOp.PutModifier;
import static com.hypixel.hytale.protocol.EntityStatOp.RemoveModifier;
import static com.hypixel.hytale.protocol.EntityStatOp.Reset;
import static com.hypixel.hytale.protocol.EntityStatOp.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StatLossTest {

    @Test
    void aFallCounts() {
        assertEquals(5.5, StatLoss.measure(List.of(update(Add)), values(10, 4.5f)), 1e-6);
    }

    @Test
    void aRiseCountsNothing() {
        assertEquals(0, StatLoss.measure(List.of(update(Add)), values(4, 10)), 1e-6);
    }

    @Test
    void aSetToZeroCountsWhatWasLeft() {
        assertEquals(68, StatLoss.measure(List.of(update(Set)), values(68, 0)), 1e-6);
    }

    @Test
    void resetsAndMaximumChangesCountNothing() {
        for (EntityStatOp op : List.of(Reset, Init, PutModifier, RemoveModifier)) {
            assertEquals(0, StatLoss.measure(List.of(update(op)), values(100, 0)), 1e-6, op.name());
        }
    }

    @Test
    void entriesAddUp() {
        assertEquals(7, StatLoss.measure(List.of(update(Add), update(Min), update(Add)), values(10, 6, 6, 9, 9, 6)), 1e-6);
    }

    @Test
    void noLogIsNoLoss() {
        assertEquals(0, StatLoss.measure(null, null), 1e-6);
    }

    private static EntityStatUpdate update(EntityStatOp op) {
        return new EntityStatUpdate(op, false, 0, null, null, null);
    }

    private static FloatArrayList values(float... values) {
        return new FloatArrayList(values);
    }
}
