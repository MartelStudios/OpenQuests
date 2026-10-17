package com.martelstudios.openquests.mmoskilltree.quests.moment;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.ziggfreed.mmoskilltree.i18n.Messages;

import javax.annotation.Nonnull;

/**
 * Counts the XP the player gains, in its skill or in any.
 */
public class GainXpQuestProgression extends MomentQuestProgression<GainXpQuestProgression> {

    public static final BuilderCodec<GainXpQuestProgression> CODEC = BuilderCodec.builder(GainXpQuestProgression.class, GainXpQuestProgression::new, MomentQuestProgression.BASE_CODEC)
                                                                                 .build();

    protected GainXpQuestProgression() {}

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        if (getAsset() == null) return super.getDefaultTitle();

        String skill = getTarget();
        if (skill == null) return Message.translation("openquestsmmoskilltree.quest.default.gain-xp-any").param("quantity", getTargetQuantity());

        return Message.translation("openquestsmmoskilltree.quest.default.gain-xp")
                      .param("quantity", getTargetQuantity())
                      .param("skill", Messages.skillNameMsg(skill));
    }
}
