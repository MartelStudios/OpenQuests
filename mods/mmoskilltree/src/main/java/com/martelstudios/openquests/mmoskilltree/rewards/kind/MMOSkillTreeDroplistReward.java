package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * One of the game's own drop lists, rolled a number of times.
 */
public class MMOSkillTreeDroplistReward extends KindReward {
    public static final BuilderCodec<MMOSkillTreeDroplistReward> CODEC = BuilderCodec.builder(MMOSkillTreeDroplistReward.class, MMOSkillTreeDroplistReward::new, KindReward.BASE_CODEC)
                                                                         .append(new KeyedCodec<>("Droplist", Codec.STRING, true), (reward, droplist) -> reward.droplist = droplist, reward -> reward.droplist)
                                                                         .addValidator(Validators.nonNull())
                                                                         .addValidatorLate(() -> ItemDropList.VALIDATOR_CACHE.getValidator().late())
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Rolls", Codec.INTEGER), (reward, rolls) -> reward.rolls = rolls, reward -> Integer.valueOf(reward.rolls))
                                                                         .addValidator(Validators.greaterThan(0))
                                                                         .add()
                                                                         .build();

    protected String droplist;

    protected int rolls = 1;

    private MMOSkillTreeDroplistReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Droplist", Map.of("droplist", droplist, "rolls", Integer.toString(rolls)));
    }
}
