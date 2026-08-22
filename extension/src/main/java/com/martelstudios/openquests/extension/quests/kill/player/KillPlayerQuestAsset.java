package com.martelstudios.openquests.extension.quests.kill.player;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

import javax.annotation.Nullable;

/**
 * Kill a number of players, optionally a designated one. The target is a username or a uuid, and
 * is left out to mean any player.
 */
public class KillPlayerQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<KillPlayerQuestAsset> CODEC =
        BuilderCodec.builder(KillPlayerQuestAsset.class, KillPlayerQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("PlayerToKill", Codec.STRING), (asset, player) -> asset.playerToKill = player, asset -> asset.playerToKill)
                    .add()
                    .build();

    @Nullable
    protected String playerToKill;

    private KillPlayerQuestAsset() {}

    @Override
    public AbstractQuestProgression<?> create() {
        return new KillPlayerQuestProgression().setAssetId(getId());
    }

    /**
     * @return the username or uuid to hunt, or {@code null} when any player counts.
     */
    @Nullable
    public String getPlayerToKill() {
        return playerToKill;
    }
}
