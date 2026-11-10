package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Moves a player along their paths: an awaited spot counts and plays its sound, and what the path
 * lists in {@code ResetOn} sends it back to its first spot.
 */
public class ReachPathQuestVisitor implements QuestVisitor<ReachPathQuestProgression> {

    private final PlayerRef player;
    private final Vector3d position;
    private final boolean onGround;

    public ReachPathQuestVisitor(@Nonnull PlayerRef player, @Nonnull Vector3d position, boolean onGround) {
        this.player = player;
        this.position = position;
        this.onGround = onGround;
    }

    @Override
    public void progress(ReachPathQuestProgression quest) {
        if (!quest.getPlayers().contains(player.getUuid()) || quest.isOver()) return;

        ReachPathQuestAsset asset = quest.getAsset();
        int spot = asset.spotAt(position);

        if (quest.apply(new ReachPathQuestProgression.Reach(spot))) {
            asset.getLocations().get(spot).playSound(player);
            return;
        }

        boolean skipped = asset.isOrdered() && spot > quest.getCurrentQuantity() && asset.resetsOn(ReachPathQuestAsset.Reset.Skip);
        boolean landed = spot < 0 && onGround && asset.resetsOn(ReachPathQuestAsset.Reset.Landing);
        if ((skipped || landed) && quest.apply(new ReachPathQuestProgression.Restart())) {
            ReachLocationQuestAsset.playSound(player, asset.getResetSound(), 1);
        }
    }

    @Override
    public Class<ReachPathQuestProgression> getQuestType() {
        return ReachPathQuestProgression.class;
    }

    @Nonnull
    @Override
    public UUID getActorId() {
        return player.getUuid();
    }
}
