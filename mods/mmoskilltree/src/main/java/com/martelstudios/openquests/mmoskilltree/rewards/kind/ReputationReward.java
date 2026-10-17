package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.builtin.adventure.reputation.assets.ReputationGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Standing with a reputation, such as the Mastery Trainer's favour, whose ranks pay mastery points
 * and open lessons on the trainer's board. A negative amount takes standing away.
 */
public class ReputationReward extends KindReward {
    public static final BuilderCodec<ReputationReward> CODEC = BuilderCodec.builder(ReputationReward.class, ReputationReward::new, KindReward.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("Reputation", Codec.STRING, true), (reward, reputation) -> reward.reputation = reputation, reward -> reward.reputation)
                                                                           .addValidator(Validators.nonNull())
                                                                           .addValidatorLate(() -> ReputationGroup.VALIDATOR_CACHE.getValidator().late())
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Amount", Codec.INTEGER, true), (reward, amount) -> reward.amount = amount, reward -> Integer.valueOf(reward.amount))
                                                                           .addValidator(Validators.notEqual(0))
                                                                           .add()
                                                                           .build();

    protected String reputation;

    protected int amount;

    private ReputationReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Reputation", Map.of("reputation", reputation, "amount", Integer.toString(amount)));
    }
}
