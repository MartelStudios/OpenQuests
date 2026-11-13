package com.martelstudios.openquests.extension.quests.quantity;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.CodecJson;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComboWindowTest {

    private static final Instant T0 = Instant.parse("2026-11-13T12:00:00Z");

    @Test
    void addsInsideTheWindowAccumulate() {
        ComboQuest quest = combo();
        quest.apply(new QuantityQuestProgression.Add(1, T0));
        quest.apply(new QuantityQuestProgression.Add(1, T0.plusSeconds(9)));

        assertEquals(2, quest.getCurrentQuantityAt(T0.plusSeconds(9)));
    }

    @Test
    void anAddAfterTheWindowRestartsTheCount() {
        ComboQuest quest = combo();
        quest.apply(new QuantityQuestProgression.Add(3, T0));
        quest.apply(new QuantityQuestProgression.Add(1, T0.plusSeconds(10)));

        assertEquals(1, quest.getCurrentQuantityAt(T0.plusSeconds(10)));
        assertEquals(T0.plusSeconds(20), quest.getComboEndsAt());
    }

    @Test
    void theCountReadsZeroOnceTheWindowCloses() {
        ComboQuest quest = combo();
        quest.apply(new QuantityQuestProgression.Add(3, T0));

        assertEquals(3, quest.getCurrentQuantityAt(T0.plusSeconds(9)));
        assertEquals(0, quest.getCurrentQuantityAt(T0.plusSeconds(10)));
    }

    @Test
    void reachingTheTargetInsideTheWindowSucceeds() {
        ComboQuest quest = combo();
        for (int i = 0; i < 5; i++) quest.apply(new QuantityQuestProgression.Add(1, T0.plusSeconds(i)));

        assertEquals(QuestState.SUCCESSFUL, quest.getState());
    }

    @Test
    void aStoredComboComesBackClosed() {
        ComboQuest quest = combo();
        quest.apply(new QuantityQuestProgression.Add(2, T0));

        ComboQuest stored = CodecJson.decode(ComboQuest.CODEC, CodecJson.encode(ComboQuest.CODEC, quest), "quest");

        assertEquals(T0.plusSeconds(10), stored.getComboEndsAt());
        assertEquals(0, stored.getCurrentQuantityAt(T0.plusSeconds(3600)));
    }

    @Test
    void mergedAddsKeepTheFirstMoment() {
        assertEquals(new QuantityQuestProgression.Add(3, T0), new QuantityQuestProgression.Add(1, T0).followedBy(new QuantityQuestProgression.Add(2, T0.plusSeconds(3))));
    }

    @Test
    void noWindowCountsAsToday() {
        ComboQuest quest = new ComboQuest.Builder(null).targetQuantity(5).build();
        quest.apply(new QuantityQuestProgression.Add(2, T0));

        assertEquals(2, quest.getCurrentQuantityAt(T0.plusSeconds(3600)));
    }

    private static ComboQuest combo() {
        return new ComboQuest.Builder(Duration.ofSeconds(10)).targetQuantity(5).build();
    }

    /**
     * A counted quest whose window is set here, standing in for an asset carrying a {@code Within}.
     */
    static final class ComboQuest extends QuantityQuestProgression<ComboQuest> {
        static final BuilderCodec<ComboQuest> CODEC = BuilderCodec.builder(ComboQuest.class, () -> new ComboQuest(Duration.ofSeconds(10)), QuantityQuestProgression.BASE_CODEC).build();

        @Nullable
        private final Duration window;

        ComboQuest(@Nullable Duration window) {
            this.window = window;
        }

        @Nullable
        @Override
        protected Duration getComboWindow() {
            return window;
        }

        static final class Builder extends QuantityQuestProgression.Builder<ComboQuest, Builder> {
            Builder(@Nullable Duration window) {
                super(new ComboQuest(window), "Combo");
            }
        }
    }
}
