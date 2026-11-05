package com.martelstudios.openquests.extension.quests.gameflag;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;

/**
 * Quest completed once its game flag reaches the asset's level.
 */
public class GameFlagQuestProgression extends AbstractQuestProgression<GameFlagQuestProgression> {

    public static final BuilderCodec<GameFlagQuestProgression> CODEC = BuilderCodec.builder(GameFlagQuestProgression.class, GameFlagQuestProgression::new, AbstractQuestProgression.BASE_CODEC)
                                                                                   .build();

    protected GameFlagQuestProgression() {}

    @Override
    public GameFlagQuestAsset getAsset() {
        return (GameFlagQuestAsset) super.getAsset();
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        var asset = getAsset();
        if (asset == null || asset.getFlag() == null) return super.getDefaultTitle();

        return Message.translation("openquests.quest.default.game-flag").param("flag", asset.getFlag());
    }
}
