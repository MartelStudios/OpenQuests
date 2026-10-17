package com.martelstudios.openquests.mmoskilltree.rewards.xp;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;
import com.ziggfreed.mmoskilltree.data.SkillComponent;
import com.ziggfreed.mmoskilltree.skill.SkillService;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.UUID;

/**
 * Whole levels in an MMO Skill Tree skill, rather than an amount of XP that means a level at one
 * point of the curve and a fraction of one further up.
 */
public class LevelReward extends QuestReward {
    public static final BuilderCodec<LevelReward> CODEC = BuilderCodec.builder(LevelReward.class, LevelReward::new, QuestReward.BASE_CODEC)
                                                                      .append(new KeyedCodec<>("Skill", Codec.STRING, true), (reward, skill) -> reward.skill = skill, reward -> reward.skill)
                                                                      .addValidator(Validators.nonNull())
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Levels", Codec.INTEGER), (reward, levels) -> reward.levels = levels, reward -> Integer.valueOf(reward.levels))
                                                                      .addValidator(Validators.greaterThan(0))
                                                                      .add()
                                                                      .build();

    /**
     * How many times the XP is topped up at most, for a multiplier below one that keeps it short.
     */
    private static final int MAX_AWARDS = 5;

    protected String skill;

    protected int levels = 1;

    private LevelReward() {}

    /**
     * Awards the XP missing to reach the start of the level that many above the current one, through
     * the same path as {@link XpReward}. A multiplier below one is made up for, one above it pays
     * extra, and a skill at its cap takes nothing and is done.
     */
    @Override
    public boolean grant(@Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        var ref = playerComponents.getReference();
        if (ref == null) return false;

        var store = ref.getStore();
        var skills = MMOSkillTreeAPI.getSkillComponent(store, ref);
        if (skills == null) return false;

        String skillId = skill.toUpperCase(Locale.ROOT);
        long target = SkillComponent.getXpForLevel(skills.getLevelByName(skillId) + levels);

        for (int i = 0; i < MAX_AWARDS; i++) {
            long missing = target - skills.getXpByName(skillId);
            if (missing <= 0 || SkillService.awardXpByName(store, ref, skills, skillId, missing) <= 0) break;
        }
        return true;
    }

    public String getSkill() {
        return skill;
    }

    public int getLevels() {
        return levels;
    }
}
