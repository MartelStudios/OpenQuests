package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * An entity effect put on the player, for the effect's own duration unless one is given.
 */
public class MMOSkillTreeEffectReward extends KindReward {
    public static final BuilderCodec<MMOSkillTreeEffectReward> CODEC = BuilderCodec.builder(MMOSkillTreeEffectReward.class, MMOSkillTreeEffectReward::new, KindReward.BASE_CODEC)
                                                                       .append(new KeyedCodec<>("Effect", Codec.STRING, true), (reward, effect) -> reward.effect = effect, reward -> reward.effect)
                                                                       .addValidator(Validators.nonNull())
                                                                       .addValidatorLate(() -> EntityEffect.VALIDATOR_CACHE.getValidator().late())
                                                                       .add()
                                                                       .append(new KeyedCodec<>("DurationSeconds", Codec.DOUBLE), (reward, seconds) -> reward.durationSeconds = seconds, reward -> reward.durationSeconds)
                                                                       .addValidator(Validators.greaterThan(0.0))
                                                                       .add()
                                                                       .build();

    protected String effect;

    /**
     * {@code null} for the effect's own duration.
     */
    protected Double durationSeconds;

    private MMOSkillTreeEffectReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        Map<String, String> params = new HashMap<>(Map.of("effect", effect));
        if (durationSeconds != null) params.put("durationseconds", durationSeconds.toString());
        return RewardSpec.of("Effect", params);
    }
}
