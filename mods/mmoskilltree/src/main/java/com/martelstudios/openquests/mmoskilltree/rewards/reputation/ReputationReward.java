package com.martelstudios.openquests.mmoskilltree.rewards.reputation;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.ziggfreed.common.reputation.ReputationRuntime;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Standing with a reputation of MMO Skill Tree's CommonLib, such as the Mastery Trainer's favour,
 * whose ranks pay mastery points and open lessons on the trainer's board. Its Mastery Pack defines
 * that one; a negative amount takes standing away.
 */
public class ReputationReward extends QuestReward {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<ReputationReward> CODEC = BuilderCodec.builder(ReputationReward.class, ReputationReward::new, QuestReward.BASE_CODEC)
                                                                           .append(new KeyedCodec<>("Reputation", Codec.STRING, true), (reward, reputation) -> reward.reputation = reputation, reward -> reward.reputation)
                                                                           .addValidator(Validators.nonNull())
                                                                           .add()
                                                                           .append(new KeyedCodec<>("Amount", Codec.INTEGER, true), (reward, amount) -> reward.amount = amount, reward -> Integer.valueOf(reward.amount))
                                                                           .addValidator(Validators.notEqual(0))
                                                                           .add()
                                                                           .build();

    protected String reputation;

    protected int amount;

    private ReputationReward() {}

    /**
     * Standing scaled or capped to nothing still counts as paid. An unknown reputation is a missing
     * pack: the reward stays pending until it is there.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        var ref = playerComponents.getReference();
        if (ref == null) return false;

        var result = ReputationRuntime.service().change(ref.getStore(), ref, reputation, amount, "OpenQuests:" + sourceQuestId);

        return switch (result.status()) {
            case CHANGED, UNCHANGED -> true;
            case UNKNOWN -> {
                LOGGER.atWarning().log("No reputation '%s' switched on: the reward of quest %s stays pending", reputation, sourceQuestId);
                yield false;
            }
            default -> false;
        };
    }

    public String getReputation() {
        return reputation;
    }

    public int getAmount() {
        return amount;
    }
}
