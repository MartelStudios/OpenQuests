package com.martelstudios.openquests.extension.quests.reachlocation;

import com.martelstudios.openquests.core.models.QuestOperation;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
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
        if (quest.isOver()) return;

        var asset = quest.getAsset();
        double radius = asset.getRadius();
        if (position.distanceSquared(asset.getPosition()) > radius * radius) return;

        quest.apply(new QuestOperation.SetState(QuestState.SUCCESSFUL));
    }

    @Override
    public Class<ReachLocationQuestProgression> getQuestType() {
        return ReachLocationQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return playerId;
    }
}
