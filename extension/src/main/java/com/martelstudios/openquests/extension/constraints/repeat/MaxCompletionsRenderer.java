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
 * The cap beside how much of it the player has used, finished quests included.
 */
public final class MaxCompletionsRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return MaxCompletionsConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        MaxCompletionsConstraint max = (MaxCompletionsConstraint) constraint;
        int used = max.getCount() - max.getRemaining(QuestPlayerStateService.get().getCompletions(context.getViewer(), asset.getId()));

        return new Line(Message.translation("openquests.page.constraint.max-completions").param("count", max.getCount()),
                        Message.raw(used + "/" + max.getCount()));
    }
}
