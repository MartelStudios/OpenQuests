package com.martelstudios.hyquests.extension.quests.kill;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.hyquests.core.models.AbstractQuestProgression;
import com.martelstudios.hyquests.extension.quests.count.CountQuestAsset;

import javax.annotation.Nullable;

/**
 * Kill a number of players, optionally a designated one. The target is a username or a uuid, the
 * same thing the player commands accept, and is left out to mean "anyone".
 */
public class KillPlayerQuestAsset extends CountQuestAsset {

    public static final BuilderCodec<KillPlayerQuestAsset> CODEC =
        BuilderCodec.builder(KillPlayerQuestAsset.class, KillPlayerQuestAsset::new, CountQuestAsset.BASE_CODEC)
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
