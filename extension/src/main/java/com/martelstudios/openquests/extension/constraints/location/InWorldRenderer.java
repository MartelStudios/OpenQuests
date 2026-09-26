package com.martelstudios.openquests.extension.constraints.location;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.journal.QuestConstraintRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Names the pattern as written, the way an {@code EnterWorld} quest names its own. An asset
 * meaning "the arena" rather than a regular expression says so through {@code DescriptionKey}.
 */
public final class InWorldRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return InWorldConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        if (QuestConstraintRenderer.isOver(quest)) return null;

        InWorldConstraint inWorld = (InWorldConstraint) constraint;
        String key = inWorld.failsOnLeave() ? "openquests.page.constraint.in-world.fail" : "openquests.page.constraint.in-world";

        return Line.of(Message.translation(key).param("world", inWorld.getWorldNamePattern()));
    }
}
