package com.martelstudios.openquests.mmoskilltree.quests.moment.completebounty;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.mmoskilltree.quests.moment.MomentQuestProgression;
import com.ziggfreed.common.board.asset.BoardConfig;
import com.ziggfreed.common.i18n.ContentKeys;

import javax.annotation.Nonnull;

/**
 * Counts the bounties collected from its board, or from any.
 */
public class CompleteBountyQuestProgression extends MomentQuestProgression<CompleteBountyQuestProgression> {

    public static final BuilderCodec<CompleteBountyQuestProgression> CODEC = BuilderCodec.builder(CompleteBountyQuestProgression.class, CompleteBountyQuestProgression::new, MomentQuestProgression.BASE_CODEC)
                                                                                         .build();

    protected CompleteBountyQuestProgression() {}

    @Nonnull
    @Override
    public Message getDefaultTitle() {
        if (getAsset() == null) return super.getDefaultTitle();

        String board = getTarget();
        var asset = board == null ? null : BoardConfig.getInstance().resolve(board);
        var text = asset == null ? null : asset.getText();
        String titleKey = text == null ? null : text.getTitleKey();
        if (titleKey == null) return Message.translation("openquestsmmoskilltree.quest.default.complete-bounty-any").param("quantity", getTargetQuantity());

        return Message.translation("openquestsmmoskilltree.quest.default.complete-bounty")
                      .param("quantity", getTargetQuantity())
                      .param("board", ContentKeys.tr(titleKey));
    }
}
