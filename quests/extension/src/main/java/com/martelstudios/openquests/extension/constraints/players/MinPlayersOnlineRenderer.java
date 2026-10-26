package com.martelstudios.openquests.extension.constraints.players;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.extension.journal.QuestConstraintRenderer;
import com.martelstudios.openquests.extension.journal.QuestPageContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The number needed, beside how many are on right now, so a player knows whether the quest moves.
 */
public final class MinPlayersOnlineRenderer implements QuestConstraintRenderer {

    @Nonnull
    @Override
    public Class<?> getConstraintType() {
        return MinPlayersOnlineConstraint.class;
    }

    @Nullable
    @Override
    public Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest) {
        if (QuestConstraintRenderer.isOver(quest)) return null;

        int count = ((MinPlayersOnlineConstraint) constraint).getCount();

        return new Line(Message.translation("openquests.page.constraint.min-players").param("count", count),
                        Message.raw(Universe.get().getPlayerCount() + "/" + count));
    }
}
