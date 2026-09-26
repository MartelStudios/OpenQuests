package com.martelstudios.openquests.extension.constraints.time;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.journal.QuestConstraintRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

/**
 * The time left on a running quest, or the whole limit on one not handed out yet.
 */
public final class TimeLimitRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return TimeLimitConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        if (QuestConstraintRenderer.isOver(quest)) return null;

        TimeLimitConstraint timeLimit = (TimeLimitConstraint) constraint;
        Instant deadline = quest == null ? null : timeLimit.getDeadline(quest);

        if (deadline == null) {
            return new Line(Message.translation("openquests.page.constraint.time-limit"), QuestPageRows.formatDuration(timeLimit.getDuration()));
        }
        return new Line(Message.translation("openquests.page.constraint.time-left"), QuestPageRows.formatDuration(Duration.between(Instant.now(), deadline)));
    }
}
