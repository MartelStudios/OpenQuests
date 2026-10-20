package com.martelstudios.openquests.mmoskilltree.rewards.level;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.mmoskilltree.editor.DataSets;
import com.martelstudios.openquests.mmoskilltree.rewards.xp.MMOSkillTreeXpReward;
import com.martelstudios.openquests.mmoskilltree.skills.SkillReference;
import com.ziggfreed.mmoskilltree.api.MMOSkillTreeAPI;
import com.ziggfreed.mmoskilltree.skill.SkillService;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.UUID;

/**
 * Whole levels in an MMO Skill Tree skill, rather than an amount of XP that means a level at one
 * point of the curve and a fraction of one further up.
 */
public class MMOSkillTreeLevelReward extends QuestReward implements SkillReference {
    public static final BuilderCodec<MMOSkillTreeLevelReward> CODEC = BuilderCodec.builder(MMOSkillTreeLevelReward.class, MMOSkillTreeLevelReward::new, QuestReward.BASE_CODEC)
                                                                      .append(new KeyedCodec<>("Skill", Codec.STRING, true), (reward, skill) -> reward.skill = skill, reward -> reward.skill)
                                                                      .addValidator(Validators.nonNull())
                                                                      .metadata(DataSets.SKILL_DROPDOWN)
                                                                      .add()
                                                                      .append(new KeyedCodec<>("Levels", Codec.INTEGER), (reward, levels) -> reward.levels = levels, reward -> Integer.valueOf(reward.levels))
                                                                      .addValidator(Validators.greaterThan(0))
                                                                      .add()
                                                                      .build();

    /**
     * The first award measures the multipliers, the second asks for what is missing divided by
     * them, the third covers rounding.
     */
    private static final int MAX_AWARDS = 3;

    protected String skill;

    protected int levels = 1;

    private MMOSkillTreeLevelReward() {}

    /**
     * Awards the XP missing to reach the start of the level that many above the current one, through
     * the same path as {@link MMOSkillTreeXpReward}. A multiplier below one is made up for, one above it pays
     * extra, and a skill at its cap takes nothing and is done.
     */
    @Override
    public boolean grant(@Nonnull OpenQuestsServices services, @Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        var ref = playerComponents.getReference();
        if (ref == null) return false;

        var store = ref.getStore();
        var skills = MMOSkillTreeAPI.getSkillComponent(store, ref);
        if (skills == null) return false;

        String skillId = skill.toUpperCase(Locale.ROOT);
        long target = MMOSkillTreeAPI.getXpRequiredForLevel(skills.getLevelByName(skillId) + levels);

        // What one award grants over what it asked: class, world, boost and rested XP multipliers
        double ratio = 1;
        for (int i = 0; i < MAX_AWARDS; i++) {
            long missing = target - skills.getXpByName(skillId);
            if (missing <= 0) break;

            long asked = (long) Math.ceil(missing / ratio);
            long granted = SkillService.awardXpByName(store, ref, skills, skillId, asked);
            if (granted <= 0) break;

            ratio = (double) granted / asked;
        }
        return true;
    }

    @Override
    public String getSkill() {
        return skill;
    }

    public int getLevels() {
        return levels;
    }
}
