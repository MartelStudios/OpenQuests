package com.martelstudios.openquests.extension.quests.triggervolume;

import com.hypixel.hytale.builtin.triggervolumes.effect.TriggerEventType;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Counts one event against the player's quests waiting for it on the volume it happened on.
 */
public class TriggerVolumeQuestVisitor implements QuestVisitor<TriggerVolumeQuestProgression> {

    private final UUID playerId;
    private final TriggerEventType eventType;
    private final String volumeName;

    public TriggerVolumeQuestVisitor(@Nonnull UUID playerId, @Nonnull TriggerEventType eventType, @Nonnull String volumeName) {
        this.playerId = playerId;
        this.eventType = eventType;
        this.volumeName = volumeName;
    }

    @Override
    public void progress(TriggerVolumeQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isOver()) return;
        if (!quest.matches(eventType, volumeName)) return;

        quest.apply(new QuantityQuestProgression.Add(1));
    }

    @Override
    public Class<TriggerVolumeQuestProgression> getQuestType() {
        return TriggerVolumeQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
