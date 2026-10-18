package com.martelstudios.openquests.mmoskilltree.rewards.currency;

import com.hypixel.hytale.assetstore.AssetKeyValidator;
import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.ValidatorCache;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.mmoskilltree.rewards.kind.KindReward;
import com.ziggfreed.common.currency.asset.CurrencyAsset;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * A currency, mastery points ({@code Mastery_Point}) first: they are what MMO Skill Tree's mastery
 * nodes cost. Its Mastery Pack defines them, MMO Skill Tree itself defines none.
 */
public class MMOSkillTreeCurrencyReward extends KindReward {
    private static final ValidatorCache<String> CURRENCIES = new ValidatorCache<>(new AssetKeyValidator<>(() -> AssetRegistry.getAssetStore(CurrencyAsset.class)));

    public static final BuilderCodec<MMOSkillTreeCurrencyReward> CODEC = BuilderCodec.builder(MMOSkillTreeCurrencyReward.class, MMOSkillTreeCurrencyReward::new, KindReward.BASE_CODEC)
                                                                         .append(new KeyedCodec<>("Currency", Codec.STRING, true), (reward, currency) -> reward.currency = currency, reward -> reward.currency)
                                                                         .addValidator(Validators.nonNull())
                                                                         .addValidatorLate(() -> CURRENCIES.getValidator().late())
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Amount", Codec.LONG, true), (reward, amount) -> reward.amount = amount, reward -> Long.valueOf(reward.amount))
                                                                         .addValidator(Validators.greaterThan(0L))
                                                                         .add()
                                                                         .build();

    protected String currency;

    protected long amount;

    private MMOSkillTreeCurrencyReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Currency", Map.of("currency", currency, "amount", Long.toString(amount)));
    }
}
