package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * A flair the player unlocks. Flairs are CommonLib configuration rather than assets, so the editor
 * cannot offer them.
 */
public class MMOSkillTreeFlairReward extends KindReward {
    public static final BuilderCodec<MMOSkillTreeFlairReward> CODEC = BuilderCodec.builder(MMOSkillTreeFlairReward.class, MMOSkillTreeFlairReward::new, KindReward.BASE_CODEC)
                                                                      .append(new KeyedCodec<>("Flair", Codec.STRING, true), (reward, flair) -> reward.flair = flair, reward -> reward.flair)
                                                                      .addValidator(Validators.nonNull())
                                                                      .add()
                                                                      .build();

    protected String flair;

    private MMOSkillTreeFlairReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Flair", Map.of("flair", flair));
    }
}
