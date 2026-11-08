package com.martelstudios.openquests.extension.quests.reachlocation;

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.martelstudios.openquests.core.visitors.QuestVisitor;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Moves a player along their paths: the next spot counts, a spot ahead of it restarts the path, and
 * so does landing outside every spot on a path that resets on a fall.
 */
public class ReachPathQuestVisitor implements QuestVisitor<ReachPathQuestProgression> {

    /**
     * How much higher each spot reached sounds, so the climb is heard.
     */
    private static final float PITCH_STEP = 0.06f;

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
        int next = quest.getCurrentQuantity();
        int spot = asset.spotAt(position);

        if (spot == next) {
            if (quest.apply(new QuantityQuestProgression.Add(1))) play(asset.getStepSound(), 1 + PITCH_STEP * spot);
        } else if (spot > next || (spot < 0 && onGround && asset.isResetOnFall())) {
            if (!quest.apply(new QuantityQuestProgression.Count(0))) return;

            play(asset.getResetSound(), 1);
            if (asset.getResetMessageKey() != null) player.sendMessage(Message.translation(asset.getResetMessageKey()));
        }
    }

    private void play(@Nullable String soundEventId, float pitch) {
        if (soundEventId == null) return;

        int index = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (index != AssetMapWithIndexes.NOT_FOUND) SoundUtil.playSoundEvent2dToPlayer(player, index, SoundCategory.UI, 1, pitch);
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
