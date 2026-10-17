package com.martelstudios.openquests.mmoskilltree.rewards.xp;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.mmoskilltree.SkillDataSets;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;
import com.ziggfreed.mmoskilltree.skill.SkillService;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.UUID;

/**
 * XP in an MMO Skill Tree skill.
 */
public class MMOSkillTreeXpReward extends QuestReward {
    public static final BuilderCodec<MMOSkillTreeXpReward> CODEC = BuilderCodec.builder(MMOSkillTreeXpReward.class, MMOSkillTreeXpReward::new, QuestReward.BASE_CODEC)
                                                                   .append(new KeyedCodec<>("Skill", Codec.STRING, true), (reward, skill) -> reward.skill = skill, reward -> reward.skill)
                                                                   .addValidator(Validators.nonNull())
                                                                   .metadata(SkillDataSets.SKILL_DROPDOWN)
                                                                   .add()
                                                                   .append(new KeyedCodec<>("Amount", Codec.LONG, true), (reward, amount) -> reward.amount = amount, reward -> Long.valueOf(reward.amount))
                                                                   .addValidator(Validators.greaterThan(0L))
                                                                   .add()
                                                                   .build();

    protected String skill;

    protected long amount;

    private MMOSkillTreeXpReward() {}

    /**
     * Awarded the way MMO Skill Tree awards its own quest XP, multipliers, level ups and the XP
     * event included: {@link MMOSkillTreeAPI#addXp} skips all three.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        var ref = playerComponents.getReference();
        if (ref == null) return false;

        var store = ref.getStore();
        var skills = MMOSkillTreeAPI.getSkillComponent(store, ref);
        if (skills == null) return false;

        SkillService.addXpByName(store, ref, skills, skill.toUpperCase(Locale.ROOT), amount);
        return true;
    }

    public String getSkill() {
        return skill;
    }

    public long getAmount() {
        return amount;
    }
}
