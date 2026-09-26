package com.martelstudios.openquests.extension.constraints.repeat;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.extension.journal.QuestConstraintRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.journal.QuestPageRows;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;

/**
 * How often the quest can be taken and, while the player is waiting, how long is left. Still drawn
 * on a quest that is over: that is when the wait matters.
 */
public final class CooldownRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return CooldownConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        CooldownConstraint cooldown = (CooldownConstraint) constraint;
        Message label = Message.translation("openquests.page.constraint.cooldown").param("duration", QuestPageRows.formatDuration(cooldown.getDuration()));

        Instant now = Instant.now();
        Instant availableAt = cooldown.getAvailableAt(QuestPlayerStateService.get().getCompletions(context.getViewer(), asset.getId()));
        if (availableAt == null || !now.isBefore(availableAt)) return Line.of(label);

        return new Line(label, Message.translation("openquests.page.constraint.cooldown.wait").param("duration", QuestPageRows.formatDuration(Duration.between(now, availableAt))));
    }
}
