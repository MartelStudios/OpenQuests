package com.martelstudios.openquests.extension.quests.movement;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

import javax.annotation.Nonnull;

/**
 * A quest counted while the player moves at one pace. {@code TargetQuantity} is a distance or a
 * duration, depending on {@code Measure}.
 */
public abstract class TravelQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<TravelQuestAsset> BASE_CODEC = BuilderCodec.abstractBuilder(TravelQuestAsset.class, QuantityQuestAsset.BASE_CODEC)
                                                                                .append(new KeyedCodec<>("Measure", new EnumCodec<>(Measure.class)), (asset, measure) -> asset.measure = measure, asset -> asset.measure)
                                                                                .add()
                                                                                .build();

    protected Measure measure = Measure.METRES;

    /**
     * @return what the counter adds up, metres unless the asset asks for time.
     */
    @Nonnull
    public Measure getMeasure() {
        return measure;
    }

    public enum Measure {
        /**
         * The ground covered at the quest's pace.
         */
        METRES,

        /**
         * The time spent moving at the quest's pace, pauses allowed: the counter waits for the
         * player to set off again.
         */
        SECONDS;

        /**
         * A player held against a wall keeps their pace without covering any ground, so time only
         * counts while they actually move.
         *
         * @return what a sample taken at the quest's pace is worth in this measure.
         */
        public double of(double metres, double seconds) {
            if (this == METRES) return metres;

            return metres > 0 ? seconds : 0;
        }
    }
}
