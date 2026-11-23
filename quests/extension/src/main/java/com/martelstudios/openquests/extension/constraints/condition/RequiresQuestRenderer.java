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
import java.util.ArrayList;
import java.util.List;

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
        List<Message> titles = new ArrayList<>();
        for (String assetId : requirement.getQuestAssetIds()) {
            if (!titles.isEmpty()) titles.add(Message.raw(", "));
            titles.add(titleOf(assetId));
        }
        return new Line(Message.join(titles.toArray(Message[]::new)), requirement.describe());
    }

    @Nonnull
    private static Message titleOf(@Nonnull String assetId) {
        OpenQuestAsset watched = OpenQuestAsset.getAsset(assetId);
        return watched == null ? Message.raw(assetId) : AbstractQuestProgression.titleOf(watched);
    }
}
