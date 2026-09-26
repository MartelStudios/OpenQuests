package com.martelstudios.openquests.extension.constraints.location;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.journal.QuestConstraintRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageContext;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The centre and the radius, in whole blocks: coordinates are what a player can walk to.
 */
public final class NearPositionRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return NearPositionConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        if (QuestConstraintRenderer.isOver(quest)) return null;

        NearPositionConstraint nearPosition = (NearPositionConstraint) constraint;
        Vector3d position = nearPosition.getPosition();

        return Line.of(Message.translation("openquests.page.constraint.near-position")
                              .param("radius", Math.round(nearPosition.getRadius()))
                              .param("x", Math.round(position.x()))
                              .param("y", Math.round(position.y()))
                              .param("z", Math.round(position.z())));
    }
}
