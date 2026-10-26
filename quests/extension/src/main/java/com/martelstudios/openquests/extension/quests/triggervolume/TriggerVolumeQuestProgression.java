package com.martelstudios.openquests.extension.quests.triggervolume;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;

/**
 * Counts the times a player triggers the asset's event on a volume whose whole name matches its pattern.
 */
public class TriggerVolumeQuestProgression extends QuantityQuestProgression<TriggerVolumeQuestProgression> {

    public static final BuilderCodec<TriggerVolumeQuestProgression> CODEC = BuilderCodec.builder(TriggerVolumeQuestProgression.class, TriggerVolumeQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                        .build();

    protected TriggerVolumeQuestProgression() {}

    @Override
    public TriggerVolumeQuestAsset getAsset() {
        return (TriggerVolumeQuestAsset) super.getAsset();
    }

    /**
     * @return {@code true} when the event is the asset's and the whole volume name matches its pattern.
     */
    public boolean matches(@Nonnull TriggerEventType eventType, @Nonnull String volumeName) {
        var asset = getAsset();
        return eventType == asset.getTriggerEventType() && asset.getVolumeNamePattern().matches(volumeName);
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        String key = getAsset().getTriggerEventType() == TriggerEventType.EXIT ? "exit" : "enter";
        return Message.translation("openquests.quest.default." + key + "-trigger-volume")
                      .param("quantity", getTargetQuantity())
                      .param("volume", getAsset().getVolumeNamePattern().getSource());
    }
}
