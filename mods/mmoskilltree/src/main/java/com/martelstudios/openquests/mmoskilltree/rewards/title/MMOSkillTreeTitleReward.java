package com.martelstudios.openquests.mmoskilltree.rewards.title;

import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.mmoskilltree.rewards.kind.KindReward;
import com.ziggfreed.common.loot.reward.RewardSpec;
import com.ziggfreed.common.objectives.title.TitleAsset;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * A title the player unlocks and can wear by their name.
 */
public class MMOSkillTreeTitleReward extends KindReward {
    private static final ValidatorCache<String> TITLES = new ValidatorCache<>(new AssetKeyValidator<>(() -> AssetRegistry.getAssetStore(TitleAsset.class)));

    public static final BuilderCodec<MMOSkillTreeTitleReward> CODEC = BuilderCodec.builder(MMOSkillTreeTitleReward.class, MMOSkillTreeTitleReward::new, KindReward.BASE_CODEC)
                                                                      .append(new KeyedCodec<>("Title", Codec.STRING, true), (reward, title) -> reward.title = title, reward -> reward.title)
                                                                      .addValidator(Validators.nonNull())
                                                                      .addValidatorLate(() -> TITLES.getValidator().late())
                                                                      .add()
                                                                      .build();

    protected String title;

    private MMOSkillTreeTitleReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Title", Map.of("title", title));
    }
}
