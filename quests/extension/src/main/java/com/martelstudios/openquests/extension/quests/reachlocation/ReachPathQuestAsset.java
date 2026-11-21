package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * Reach several spots, each written like a {@code ReachLocation}: in the order listed unless
 * {@code Ordered: false}. {@code ResetOn} lists what sends the count back to zero. The count is
 * the number of spots, whatever {@code TargetQuantity} says.
 */
public class ReachPathQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<ReachPathQuestAsset> CODEC =
        BuilderCodec.builder(ReachPathQuestAsset.class, ReachPathQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Locations", new ArrayCodec<>(ReachLocationQuestAsset.CODEC, ReachLocationQuestAsset[]::new)), (asset, locations) -> asset.locations = locations, asset -> asset.locations)
                    .addValidator(Validators.nonEmptyArray())
                    .documentation("Destinations to reach, with at least one required.")
                    .add()
                    .append(new KeyedCodec<>("Ordered", Codec.BOOLEAN), (asset, ordered) -> asset.ordered = ordered, asset -> Boolean.valueOf(asset.ordered))
                    .documentation("Requires destinations to be reached in the listed order, true by default.")
                    .add()
                    .append(new KeyedCodec<>("ResetOn", new ArrayCodec<>(new EnumCodec<>(Reset.class), Reset[]::new)), (asset, resets) -> asset.resetOn = resets, asset -> asset.resetOn)
                    .documentation("Events that clear progress, with no reset events by default.")
                    .add()
                    .append(new KeyedCodec<>("ResetSound", Codec.STRING), (asset, sound) -> asset.resetSound = sound, asset -> asset.resetSound)
                    .addValidator(SoundEvent.VALIDATOR_CACHE.getValidator())
                    .documentation("Sound event played when the path resets, with no sound by default.")
                    .add()
                    .documentation("Defines a quest that visits several destinations.")
                    .build();

    protected ReachLocationQuestAsset[] locations = new ReachLocationQuestAsset[0];
    protected boolean ordered = true;
    protected Reset[] resetOn = new Reset[0];
    @Nullable
    protected String resetSound;

    ReachPathQuestAsset() {}

    @Override
    public QuantityQuestProgression.Builder<ReachPathQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new ReachPathQuestProgression(), getId());
    }

    /**
     * @return the number of spots, the only count a path has.
     */
    @Override
    public int getTargetQuantity() {
        return locations.length;
    }

    /**
     * @return the index of the first spot whose radius holds that position, or {@code -1} for none.
     */
    public int spotAt(@Nonnull Vector3d position) {
        for (int i = 0; i < locations.length; i++) {
            if (locations[i].contains(position)) return i;
        }
        return -1;
    }

    /**
     * @return the spots, in the order written.
     */
    @Nonnull
    public List<ReachLocationQuestAsset> getLocations() {
        return Arrays.asList(locations);
    }

    /**
     * @return whether the spots must be reached in the order written.
     */
    public boolean isOrdered() {
        return ordered;
    }

    /**
     * @return whether that event sends the count back to zero.
     */
    public boolean resetsOn(@Nonnull Reset reset) {
        return Arrays.asList(resetOn).contains(reset);
    }

    /**
     * @return the sound played when the count goes back to zero.
     */
    @Nullable
    public String getResetSound() {
        return resetSound;
    }

    /**
     * What may send the count back to zero.
     */
    public enum Reset {
        /** Reaching a spot ahead of the next one, on an ordered path. */
        Skip,
        /** Standing on solid ground outside every spot. */
        Landing
    }
}
