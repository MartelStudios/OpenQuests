package com.martelstudios.openquests.mmoskilltree.rewards.costume;

import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.martelstudios.openquests.mmoskilltree.rewards.kind.KindReward;
import com.ziggfreed.common.loot.LootableAsset;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * A costume put on the player, a costume being an entity effect that changes how they look.
 * CommonLib refuses one the player cannot wear (not a costume, or another already on), and then
 * rolls the fallback loot table instead, when one is given.
 */
public class MMOSkillTreeCostumeReward extends KindReward {
    private static final ValidatorCache<String> LOOTABLES = new ValidatorCache<>(new AssetKeyValidator<>(() -> AssetRegistry.getAssetStore(LootableAsset.class)));

    public static final BuilderCodec<MMOSkillTreeCostumeReward> CODEC = BuilderCodec.builder(MMOSkillTreeCostumeReward.class, MMOSkillTreeCostumeReward::new, KindReward.BASE_CODEC)
                                                                        .append(new KeyedCodec<>("Costume", Codec.STRING, true), (reward, costume) -> reward.costume = costume, reward -> reward.costume)
                                                                        .addValidator(Validators.nonNull())
                                                                        .addValidatorLate(() -> EntityEffect.VALIDATOR_CACHE.getValidator().late())
                                                                        .add()
                                                                        .append(new KeyedCodec<>("FallbackLootable", Codec.STRING), (reward, lootable) -> reward.fallbackLootable = lootable, reward -> reward.fallbackLootable)
                                                                        .addValidatorLate(() -> LOOTABLES.getValidator().late())
                                                                        .add()
                                                                        .build();

    protected String costume;

    /**
     * Rolled instead when the costume cannot be put on, {@code null} for nothing.
     */
    @Nullable
    protected String fallbackLootable;

    private MMOSkillTreeCostumeReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        Map<String, String> params = new HashMap<>(Map.of("costume", costume));
        if (fallbackLootable != null) params.put("fallbacklootable", fallbackLootable);
        return RewardSpec.of("Costume", params);
    }
}
