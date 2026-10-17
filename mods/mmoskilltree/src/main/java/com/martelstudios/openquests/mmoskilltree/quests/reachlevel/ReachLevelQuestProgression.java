package com.martelstudios.openquests.mmoskilltree.quests.reachlevel;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;
import com.ziggfreed.mmoskilltree.i18n.Messages;
import com.ziggfreed.mmoskilltree.skill.SkillRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Counts the player's level in the skill, rechecked whenever it changes.
 */
public class ReachLevelQuestProgression extends QuantityQuestProgression<ReachLevelQuestProgression> {

    public static final BuilderCodec<ReachLevelQuestProgression> CODEC = BuilderCodec.builder(ReachLevelQuestProgression.class, ReachLevelQuestProgression::new, QuantityQuestProgression.BASE_CODEC)
                                                                                     .append(new KeyedCodec<>("Skill", Codec.STRING), (quest, skill) -> quest.skill = skill, quest -> quest.skill)
                                                                                     .add()
                                                                                     .build();

    /**
     * Overrides the asset's skill for this instance alone.
     */
    @Nullable
    private String skill;

    protected ReachLevelQuestProgression() {}

    @Override
    public ReachLevelQuestAsset getAsset() {
        return (ReachLevelQuestAsset) super.getAsset();
    }

    /**
     * @return this instance's skill if one was set on it, the asset's otherwise.
     */
    @Nonnull
    public String getSkill() {
        return skill != null ? skill : getAsset().getSkill();
    }

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        if (getAsset() == null) return super.getDefaultTitle();

        return Message.translation("openquestsmmoskilltree.quest.default.reach-level")
                      .param("quantity", getTargetQuantity())
                      .param("skill", Messages.skillNameMsg(getSkill()));
    }

    /**
     * Sets up a reach level quest, whose skill can be overridden for that quest alone.
     */
    public static final class Builder extends QuantityQuestProgression.Builder<ReachLevelQuestProgression, Builder> {
        Builder(@Nonnull ReachLevelQuestAsset asset) {
            super(new ReachLevelQuestProgression(), asset.getId());
        }

        /**
         * Overrides the asset's skill for this quest alone.
         */
        public Builder skill(@Nullable String skill) {
            quest().skill = skill;
            return this;
        }

        @Override
        protected void validate(@Nonnull ReachLevelQuestProgression quest) {
            super.validate(quest);

            if (quest.skill != null && !SkillRegistry.getInstance().isKnownSkill(quest.skill)) throw new IllegalStateException("No MMO Skill Tree skill '" + quest.skill + "'");
        }
    }
}
