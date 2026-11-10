package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
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
    private final PlayerRef player;
    private final Vector3d position;

    public ReachLocationQuestVisitor(@Nonnull PlayerRef player, @Nonnull Vector3d position) {
        this.player = player;
        this.position = position;
    }

    @Override
    public void progress(ReachLocationQuestProgression quest) {
        if (!quest.getPlayers().contains(player.getUuid())) return;
        if (quest.isOver()) return;

        var asset = quest.getAsset();
        if (!asset.contains(position)) return;

        if (quest.apply(new AbstractQuestProgression.SetState(QuestState.SUCCESSFUL))) asset.playSound(player);
    }

    @Override
    public Class<ReachLocationQuestProgression> getQuestType() {
        return ReachLocationQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return player.getUuid();
    }
}
