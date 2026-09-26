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
 * How long until the moment every quest from the asset ends, counted rather than dated: a date
 * would need the player's time zone, which the server does not know.
 */
public final class DeadlineRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return DeadlineConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        if (QuestConstraintRenderer.isOver(quest)) return null;

        Instant now = Instant.now();
        Instant at = ((DeadlineConstraint) constraint).getAt();
        if (!now.isBefore(at)) return null;

        return new Line(Message.translation("openquests.page.constraint.deadline"), QuestPageRows.formatDuration(Duration.between(now, at)));
    }
}
