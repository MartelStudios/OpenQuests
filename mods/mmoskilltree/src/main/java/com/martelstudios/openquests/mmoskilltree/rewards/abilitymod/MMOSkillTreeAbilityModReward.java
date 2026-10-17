package com.martelstudios.openquests.mmoskilltree.rewards.abilitymod;

import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.mmoskilltree.rewards.kind.KindReward;
import com.ziggfreed.common.loot.reward.RewardSpec;
import com.ziggfreed.mmoskilltree.asset.type.AbilityModAsset;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * An MMO Skill Tree ability modifier, changing how one of the player's abilities behaves.
 */
public class MMOSkillTreeAbilityModReward extends KindReward {
    private static final ValidatorCache<String> MODS = new ValidatorCache<>(new AssetKeyValidator<>(() -> AssetRegistry.getAssetStore(AbilityModAsset.class)));

    public static final BuilderCodec<MMOSkillTreeAbilityModReward> CODEC = BuilderCodec.builder(MMOSkillTreeAbilityModReward.class, MMOSkillTreeAbilityModReward::new, KindReward.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("ModId", Codec.STRING, true), (reward, modId) -> reward.modId = modId, reward -> reward.modId)
                                                                           .addValidator(Validators.nonNull())
                                                                           .addValidatorLate(() -> MODS.getValidator().late())
                                                                           .add()
                                                                           .build();

    protected String modId;

    private MMOSkillTreeAbilityModReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Mmo_Ability_Mod", Map.of("modid", modId));
    }
}
