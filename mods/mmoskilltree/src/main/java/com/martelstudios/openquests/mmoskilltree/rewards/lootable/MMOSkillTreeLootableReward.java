package com.martelstudios.openquests.mmoskilltree.rewards.lootable;

import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.mmoskilltree.rewards.kind.KindReward;
import com.ziggfreed.common.loot.LootableAsset;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * A CommonLib loot table, rolled as the reward is paid, such as the Mastery Pack's offerings.
 */
public class MMOSkillTreeLootableReward extends KindReward {
    private static final ValidatorCache<String> LOOTABLES = new ValidatorCache<>(new AssetKeyValidator<>(() -> AssetRegistry.getAssetStore(LootableAsset.class)));

    public static final BuilderCodec<MMOSkillTreeLootableReward> CODEC = BuilderCodec.builder(MMOSkillTreeLootableReward.class, MMOSkillTreeLootableReward::new, KindReward.BASE_CODEC)
                                                                         .append(new KeyedCodec<>("Lootable", Codec.STRING, true), (reward, lootable) -> reward.lootable = lootable, reward -> reward.lootable)
                                                                         .addValidator(Validators.nonNull())
                                                                         .addValidatorLate(() -> LOOTABLES.getValidator().late())
                                                                         .add()
                                                                         .build();

    protected String lootable;

    private MMOSkillTreeLootableReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Lootable", Map.of("lootable", lootable));
    }
}
