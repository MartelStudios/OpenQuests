package com.martelstudios.openquests.mmoskilltree.quests.moment.abilitycast;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.mmoskilltree.editor.DataSets;
import com.martelstudios.openquests.mmoskilltree.quests.moment.MMOSkillTreeMomentQuestAsset;
import com.martelstudios.openquests.mmoskilltree.quests.moment.MomentQuestProgression;
import com.martelstudios.openquests.mmoskilltree.skills.SkillReference;

import javax.annotation.Nullable;

/**
 * Cast an ability a number of times: one ability, any ability of a skill, or any at all.
 */
public class MMOSkillTreeAbilityCastQuestAsset extends MMOSkillTreeMomentQuestAsset implements SkillReference {

    public static final BuilderCodec<MMOSkillTreeAbilityCastQuestAsset> CODEC =
        BuilderCodec.builder(MMOSkillTreeAbilityCastQuestAsset.class, MMOSkillTreeAbilityCastQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Ability", Codec.STRING), (asset, ability) -> asset.ability = ability, asset -> asset.ability)
                    .metadata(DataSets.ABILITY_DROPDOWN)
                    .add()
                    .append(new KeyedCodec<>("Skill", Codec.STRING), (asset, skill) -> asset.skill = skill, asset -> asset.skill)
                    .metadata(DataSets.SKILL_DROPDOWN)
                    .add()
                    .build();

    @Nullable
    protected String ability;

    /**
     * The skill whose tree holds the ability, MMO Skill Tree's own qualifier of a cast.
     */
    @Nullable
    protected String skill;

    private MMOSkillTreeAbilityCastQuestAsset() {}

    @Override
    public MomentQuestProgression.Builder<AbilityCastQuestProgression, ?> builder() {
        return MomentQuestProgression.Builder.of(new AbilityCastQuestProgression(), getId());
    }

    @Nullable
    @Override
    public String getTarget() {
        return ability;
    }

    @Nullable
    @Override
    public String getQualifier() {
        return skill;
    }

    @Nullable
    @Override
    public String getSkill() {
        return skill;
    }
}
