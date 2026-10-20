package com.martelstudios.openquests.mmoskilltree.quests.moment.completebounty;

import com.martelstudios.openquests.core.utils.Players;
import com.martelstudios.openquests.mmoskilltree.quests.moment.Moments;
import com.ziggfreed.common.quest.event.QuestClaimedEvent;
import com.ziggfreed.mmoskilltree.quest.MmoBounties;

import javax.annotation.Nonnull;

/**
 * Counts bounties for {@link CompleteBountyQuestProgression}s.
 */
public final class CompleteBountyEvents {

    private final Moments moments;
    private final Players players;

    public CompleteBountyEvents(@Nonnull Moments moments, @Nonnull Players players) {
        this.moments = moments;
        this.players = players;
    }

    /**
     * A bounty is one of CommonLib's quests, counted once collected, when MMO Skill Tree counts it.
     */
    public void handleQuestClaimed(@Nonnull QuestClaimedEvent event) {
        String questId = event.questId();
        if (!MmoBounties.isBounty(questId)) return;

        // ponytail: a bounty listed on several boards counts for its first board only
        String board = MmoBounties.primaryBoardOf(questId);
        if (board == null) return;

        String difficulty = MmoBounties.difficultyOf(questId);
        players.update(event.playerId(), components -> {
            var ref = components.getReference();
            if (ref != null) moments.count(ref, event.playerId(), CompleteBountyQuestProgression.class, board, difficulty.isEmpty() ? null : difficulty, 1);
        });
    }
}
