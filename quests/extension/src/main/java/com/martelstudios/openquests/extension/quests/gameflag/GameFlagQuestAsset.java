package com.martelstudios.openquests.extension.quests.gameflag;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;
import java.util.function.ToIntFunction;

/**
 * Reach a level of a server game flag, such as a goblin lab device being repaired. Flags belong to
 * the whole universe, so one player raising it completes the quest for everyone holding it.
 */
public class GameFlagQuestAsset extends OpenQuestAsset {

    public static final BuilderCodec<GameFlagQuestAsset> CODEC =
        BuilderCodec.builder(GameFlagQuestAsset.class, GameFlagQuestAsset::new, OpenQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Flag", Codec.STRING, true), (asset, flag) -> asset.flag = flag, asset -> asset.flag)
                    .addValidator(Validators.nonNull())
                    .addValidator(Validators.nonEmptyString())
                    .add()
                    .append(new KeyedCodec<>("Level", Codec.INTEGER), (asset, level) -> asset.level = level, asset -> asset.level)
                    .addValidator(Validators.min(1))
                    .add()
                    .build();

    protected String flag;
    protected int level = 1;

    private GameFlagQuestAsset() {}

    @Override
    public AbstractQuestProgression.Builder<GameFlagQuestProgression, ?> builder() {
        return AbstractQuestProgression.Builder.of(new GameFlagQuestProgression(), getId());
    }

    /**
     * @return the flag key, as {@code /gameflags} lists it.
     */
    public String getFlag() {
        return flag;
    }

    /**
     * @param levels the current level of a flag by key, 0 for one never raised.
     */
    public boolean isReached(@Nonnull ToIntFunction<String> levels) {
        return levels.applyAsInt(flag) >= level;
    }
}
