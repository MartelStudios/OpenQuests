package com.martelstudios.openquests.extension.quests.reachlocation;

import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import org.joml.Vector3d;

import java.util.UUID;

/**
 * Progresses {@link ReachLocationQuestProgression}s by checking the player's current position against the
 * asset's target position and radius.
 */
public class ReachLocationQuestVisitor implements QuestVisitor<ReachLocationQuestProgression> {
    private final UUID playerId;
    private final Vector3d position;

    public ReachLocationQuestVisitor(UUID playerId, Vector3d position) {
        this.playerId = playerId;
        this.position = position;
    }

    @Override
    public void progress(ReachLocationQuestProgression quest) {
        if (!quest.getPlayers().contains(playerId)) return;
        if (quest.isCompleted() && quest.isStopOnComplete()) return;

        var asset = quest.getAsset();
        double radius = asset.getRadius();
        if (position.distanceSquared(asset.getPosition()) > radius * radius) return;

        quest.setState(QuestState.SUCCESSFUL);
        quest.markDirty();
    }

    @Override
    public Class<ReachLocationQuestProgression> getQuestType() {
        return ReachLocationQuestProgression.class;
    }
}
