package com.martelstudios.openquests.mmoskilltree.rewards.boosttoken;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.mmoskilltree.editor.DataSets;
import com.martelstudios.openquests.mmoskilltree.rewards.kind.KindReward;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * An MMO Skill Tree XP boost token, multiplying the XP of a skill, a category or {@code ALL} for a
 * while once the player uses it.
 */
public class MMOSkillTreeBoostTokenReward extends KindReward {
    public static final BuilderCodec<MMOSkillTreeBoostTokenReward> CODEC = BuilderCodec.builder(MMOSkillTreeBoostTokenReward.class, MMOSkillTreeBoostTokenReward::new, KindReward.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("Skill", Codec.STRING), (reward, skill) -> reward.skill = skill, reward -> reward.skill)
                                                                           .metadata(DataSets.SKILL_POOL_DROPDOWN)
                                                                           .documentation("Skill or category whose XP is boosted, defaulting to ALL.")
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Multiplier", Codec.DOUBLE, true), (reward, multiplier) -> reward.multiplier = multiplier, reward -> Double.valueOf(reward.multiplier))
                                                                           .addValidator(Validators.greaterThan(1.0))
                                                                           .documentation("Factor applied to XP while the boost is active.")
                                                                           .add()
                                                                           .append(new KeyedCodec<>("DurationMinutes", Codec.INTEGER, true), (reward, minutes) -> reward.durationMinutes = minutes, reward -> Integer.valueOf(reward.durationMinutes))
                                                                           .addValidator(Validators.greaterThan(0))
                                                                           .documentation("Number of minutes the boost lasts after the player uses the token.")
                                                                           .add()
                                                                           .documentation("Grants a token the player can use to activate a temporary XP boost.")
                                                                           .build();

    protected String skill = "ALL";

    protected double multiplier;

    protected int durationMinutes;

    private MMOSkillTreeBoostTokenReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Mmo_Boost_Token", Map.of("skill", skill, "multiplier", Double.toString(multiplier), "durationminutes", Integer.toString(durationMinutes)));
    }
}
