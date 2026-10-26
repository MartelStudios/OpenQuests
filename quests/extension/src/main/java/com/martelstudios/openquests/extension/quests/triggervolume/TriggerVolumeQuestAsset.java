package com.martelstudios.openquests.extension.quests.triggervolume;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.utils.WorldNamePattern;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Trigger an event on trigger volumes whose name matches a regular expression, a number of times.
 */
public class TriggerVolumeQuestAsset extends QuantityQuestAsset {

    /**
     * The only events the trigger volume plugin puts on the server bus, both naming the entity.
     */
    public static final Set<TriggerEventType> COUNTED_EVENT_TYPES = Set.of(TriggerEventType.ENTER, TriggerEventType.EXIT);

    // The pattern reads as a world name pattern: the same whole-name regex, kept with its error for the boot check
    public static final BuilderCodec<TriggerVolumeQuestAsset> CODEC =
        BuilderCodec.builder(TriggerVolumeQuestAsset.class, TriggerVolumeQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("VolumeNamePattern", WorldNamePattern.CODEC, true), (asset, pattern) -> asset.volumeNamePattern = pattern, asset -> asset.volumeNamePattern)
                    .addValidator(Validators.nonNull())
                    .add()
                    .append(new KeyedCodec<>("TriggerEventType", TriggerEventType.CODEC), (asset, type) -> asset.triggerEventType = type, asset -> asset.triggerEventType)
                    .add()
                    .build();

    protected WorldNamePattern volumeNamePattern;
    protected TriggerEventType triggerEventType = TriggerEventType.ENTER;

    private TriggerVolumeQuestAsset() {}

    @Override
    public QuantityQuestProgression.Builder<TriggerVolumeQuestProgression, ?> builder() {
        return QuantityQuestProgression.Builder.of(new TriggerVolumeQuestProgression(), getId());
    }

    public WorldNamePattern getVolumeNamePattern() {
        return volumeNamePattern;
    }

    public TriggerEventType getTriggerEventType() {
        return triggerEventType;
    }

    /**
     * @return why this asset can never count, {@code null} when it can.
     */
    @Nullable
    public String findError() {
        if (volumeNamePattern == null) return "has no VolumeNamePattern";
        if (volumeNamePattern.getError() != null) return "has an invalid VolumeNamePattern: " + volumeNamePattern.getError();
        if (!COUNTED_EVENT_TYPES.contains(triggerEventType)) return "has TriggerEventType " + triggerEventType + ", which never reaches the server event bus; use ENTER or EXIT";
        return null;
    }
}
