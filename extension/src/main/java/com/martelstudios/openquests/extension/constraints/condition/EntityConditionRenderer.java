package com.martelstudios.openquests.extension.constraints.condition;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.journal.QuestConstraintRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Only says there are conditions: the game's conditions carry no text a player could read, so
 * an asset names its own through {@code DescriptionKey}.
 */
public final class EntityConditionRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return EntityConditionConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        if (QuestConstraintRenderer.isOver(quest)) return null;

        return Line.of(Message.translation("openquests.page.constraint.entity-condition"));
    }
}
