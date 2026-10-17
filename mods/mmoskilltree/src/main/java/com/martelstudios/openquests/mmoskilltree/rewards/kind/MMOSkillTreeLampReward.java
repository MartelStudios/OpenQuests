package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.mmoskilltree.SkillDataSets;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * An MMO Skill Tree skill lamp: the player rubs it and picks which skills its XP goes to, among a
 * skill, a category or {@code ALL}.
 */
public class MMOSkillTreeLampReward extends KindReward {
    public static final BuilderCodec<MMOSkillTreeLampReward> CODEC = BuilderCodec.builder(MMOSkillTreeLampReward.class, MMOSkillTreeLampReward::new, KindReward.BASE_CODEC)
                                                                     .append(new KeyedCodec<>("Amount", Codec.LONG, true), (reward, amount) -> reward.amount = amount, reward -> Long.valueOf(reward.amount))
                                                                     .addValidator(Validators.greaterThan(0L))
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Picks", Codec.INTEGER), (reward, picks) -> reward.picks = picks, reward -> Integer.valueOf(reward.picks))
                                                                     .addValidator(Validators.greaterThan(0))
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Skills", Codec.STRING), (reward, skills) -> reward.skills = skills, reward -> reward.skills)
                                                                     .metadata(SkillDataSets.SKILL_POOL_DROPDOWN)
                                                                     .add()
                                                                     .build();

    protected long amount;

    protected int picks = 1;

    protected String skills = "ALL";

    private MMOSkillTreeLampReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Mmo_Lamp", Map.of("amount", Long.toString(amount), "picks", Integer.toString(picks), "skills", skills));
    }
}
