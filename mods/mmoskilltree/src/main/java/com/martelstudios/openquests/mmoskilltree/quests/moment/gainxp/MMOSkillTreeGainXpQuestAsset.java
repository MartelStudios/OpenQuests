package com.martelstudios.openquests.mmoskilltree.quests.moment.gainxp;

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
 * Gain an amount of XP, in one skill or in any. {@code TargetQuantity} is the XP, counted as
 * awarded, multipliers included.
 */
public class MMOSkillTreeGainXpQuestAsset extends MMOSkillTreeMomentQuestAsset implements SkillReference {

    public static final BuilderCodec<MMOSkillTreeGainXpQuestAsset> CODEC =
        BuilderCodec.builder(MMOSkillTreeGainXpQuestAsset.class, MMOSkillTreeGainXpQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Skill", Codec.STRING), (asset, skill) -> asset.skill = skill, asset -> asset.skill)
                    .metadata(DataSets.SKILL_DROPDOWN)
                    .add()
                    .build();

    @Nullable
    protected String skill;

    private MMOSkillTreeGainXpQuestAsset() {}

    @Override
    public MomentQuestProgression.Builder<GainXpQuestProgression, ?> builder() {
        return MomentQuestProgression.Builder.of(new GainXpQuestProgression(), getId());
    }

    @Nullable
    @Override
    public String getTarget() {
        return skill;
    }

    @Nullable
    @Override
    public String getSkill() {
        return skill;
    }
}
