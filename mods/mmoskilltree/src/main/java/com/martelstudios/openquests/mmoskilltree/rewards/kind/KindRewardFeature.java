package com.martelstudios.openquests.mmoskilltree.rewards.kind;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.extension.journal.QuestPageService;

import javax.annotation.Nonnull;

/**
 * Rewards paid through MMO Skill Tree's CommonLib, one type per reward kind its own quests pay.
 */
public final class KindRewardFeature {

    private KindRewardFeature() {}

    /**
     * Registers every reward and the journal line they share.
     */
    public static void register(@Nonnull JavaPlugin plugin) {
        register("MMOSkillTreeCurrency", MMOSkillTreeCurrencyReward.class, MMOSkillTreeCurrencyReward.CODEC);
        register("MMOSkillTreeReputation", MMOSkillTreeReputationReward.class, MMOSkillTreeReputationReward.CODEC);
        register("MMOSkillTreeLootable", MMOSkillTreeLootableReward.class, MMOSkillTreeLootableReward.CODEC);
        register("MMOSkillTreeDroplist", MMOSkillTreeDroplistReward.class, MMOSkillTreeDroplistReward.CODEC);
        register("MMOSkillTreeEffect", MMOSkillTreeEffectReward.class, MMOSkillTreeEffectReward.CODEC);
        register("MMOSkillTreeCostume", MMOSkillTreeCostumeReward.class, MMOSkillTreeCostumeReward.CODEC);
        register("MMOSkillTreeTitle", MMOSkillTreeTitleReward.class, MMOSkillTreeTitleReward.CODEC);
        register("MMOSkillTreeFlair", MMOSkillTreeFlairReward.class, MMOSkillTreeFlairReward.CODEC);
        register("MMOSkillTreeBoostToken", MMOSkillTreeBoostTokenReward.class, MMOSkillTreeBoostTokenReward.CODEC);
        register("MMOSkillTreeLamp", MMOSkillTreeLampReward.class, MMOSkillTreeLampReward.CODEC);
        register("MMOSkillTreeAbilityMod", MMOSkillTreeAbilityModReward.class, MMOSkillTreeAbilityModReward.CODEC);

        QuestPageService.register(new KindRewardRenderer());
    }

    private static <R extends KindReward> void register(@Nonnull String typeId, @Nonnull Class<R> type, @Nonnull BuilderCodec<R> codec) {
        QuestReward.CODEC.register(typeId, type, codec);
    }
}
