package com.martelstudios.openquests.mmoskilltree.quests.moment;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.ziggfreed.mmoskilltree.ability.catalog.AbilityCatalog;
import com.ziggfreed.mmoskilltree.i18n.Messages;

import javax.annotation.Nonnull;

/**
 * Counts the casts of its ability, of any ability of its skill, or of any.
 */
public class AbilityCastQuestProgression extends MomentQuestProgression<AbilityCastQuestProgression> {

    public static final BuilderCodec<AbilityCastQuestProgression> CODEC = BuilderCodec.builder(AbilityCastQuestProgression.class, AbilityCastQuestProgression::new, MomentQuestProgression.BASE_CODEC)
                                                                                      .build();

    protected AbilityCastQuestProgression() {}

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        if (getAsset() == null) return super.getDefaultTitle();

        String ability = getTarget();
        var asset = ability == null ? null : AbilityCatalog.getInstance().getAbility(ability);
        if (asset != null) {
            return Message.translation("openquestsmmoskilltree.quest.default.ability-cast")
                          .param("quantity", getTargetQuantity())
                          .param("ability", Messages.abilityNameMsg(asset));
        }

        String skill = getQualifier();
        if (skill != null) {
            return Message.translation("openquestsmmoskilltree.quest.default.ability-cast-skill")
                          .param("quantity", getTargetQuantity())
                          .param("skill", Messages.skillNameMsg(skill));
        }

        return Message.translation("openquestsmmoskilltree.quest.default.ability-cast-any").param("quantity", getTargetQuantity());
    }
}
