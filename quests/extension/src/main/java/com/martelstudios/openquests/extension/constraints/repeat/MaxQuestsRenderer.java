package com.martelstudios.openquests.extension.constraints.repeat;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.extension.journal.QuestConstraintRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The cap beside how much of it the player has used, quests still running included.
 */
public final class MaxQuestsRenderer implements QuestConstraintRenderer {

    private final QuestPlayerStateService playerState;

    public MaxQuestsRenderer(@Nonnull QuestPlayerStateService playerState) {
        this.playerState = playerState;
    }

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return MaxQuestsConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        MaxQuestsConstraint max = (MaxQuestsConstraint) constraint;
        int used = max.getCount() - max.getRemaining(playerState.getTally(context.getViewer(), asset.getId()));
        String key = max.isAtOnce() ? "openquests.page.constraint.max-quests-at-once" : "openquests.page.constraint.max-quests";
        return new Line(Message.translation(key).param("count", max.getCount()), Message.raw(used + "/" + max.getCount()));
    }
}
