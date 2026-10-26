package com.martelstudios.openquests.extension.constraints.condition;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.journal.QuestConstraintRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import com.martelstudios.openquests.extension.quests.queststate.QuestRequirement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The quest waited on, beside what is asked of it.
 */
public final class RequiresQuestRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return RequiresQuestConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        if (QuestConstraintRenderer.isOver(quest)) return null;

        QuestRequirement requirement = ((RequiresQuestConstraint) constraint).getRequirement();
        OpenQuestAsset watched = OpenQuestAsset.getAsset(requirement.getQuestAssetId());
        Message title = watched == null ? Message.raw(requirement.getQuestAssetId()) : AbstractQuestProgression.titleOf(watched);
        return new Line(title, requirement.describe());
    }
}
