package com.martelstudios.openquests.mmoskilltree.rewards.costume;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.martelstudios.openquests.mmoskilltree.rewards.kind.KindReward;
import com.ziggfreed.common.loot.reward.RewardSpec;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * A costume put on the player, a costume being an entity effect that changes how they look.
 */
public class MMOSkillTreeCostumeReward extends KindReward {
    public static final BuilderCodec<MMOSkillTreeCostumeReward> CODEC = BuilderCodec.builder(MMOSkillTreeCostumeReward.class, MMOSkillTreeCostumeReward::new, KindReward.BASE_CODEC)
                                                                        .append(new KeyedCodec<>("Costume", Codec.STRING, true), (reward, costume) -> reward.costume = costume, reward -> reward.costume)
                                                                        .addValidator(Validators.nonNull())
                                                                        .addValidatorLate(() -> EntityEffect.VALIDATOR_CACHE.getValidator().late())
                                                                        .add()
                                                                        .build();

    protected String costume;

    private MMOSkillTreeCostumeReward() {}

    @Nonnull
    @Override
    public RewardSpec toSpec() {
        return RewardSpec.of("Costume", Map.of("costume", costume));
    }
}
