package com.martelstudios.openquests.mmoskilltree.quests.reachlevel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;
import com.martelstudios.openquests.mmoskilltree.DataSets;

/**
 * Reach a level in an MMO Skill Tree skill. {@code TargetQuantity} is the level, so the counter
 * every counted quest draws shows how far the player is.
 */
public class MMOSkillTreeReachLevelQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<MMOSkillTreeReachLevelQuestAsset> CODEC =
        BuilderCodec.builder(MMOSkillTreeReachLevelQuestAsset.class, MMOSkillTreeReachLevelQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("Skill", Codec.STRING, true), (asset, skill) -> asset.skill = skill, asset -> asset.skill)
                    .addValidator(Validators.nonNull())
                    .metadata(DataSets.SKILL_DROPDOWN)
                    .add()
                    .build();

    protected String skill;

    private MMOSkillTreeReachLevelQuestAsset() {}

    @Override
    public ReachLevelQuestProgression.Builder builder() {
        return new ReachLevelQuestProgression.Builder(this);
    }

    /**
     * @return the skill id as written, a built-in one ({@code Mining}) or a custom one.
     */
    public String getSkill() {
        return skill;
    }
}
