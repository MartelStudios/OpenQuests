package com.martelstudios.openquests.extension.journal;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.OpenQuestAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * How one constraint type tells the player about itself in the journal, as one line or none.
 * Registered the way a reward type registers its own; the asset can still put its own words on the
 * line through the constraint's {@code DescriptionKey}.
 */
public interface QuestConstraintRenderer {

    /**
     * @return the constraint class this renders. Subclasses fall back to it.
     */
    @Nonnull
    Class<?> getConstraintType();

    /**
     * @param quest the run the journal is showing, {@code null} for an asset the player has not
     * been handed yet
     * @return the line, or {@code null} for a rule that no longer concerns the player, such as the
     * time left on a quest that is over.
     */
    @Nullable
    Line describe(@Nonnull QuestPageContext context, @Nonnull QuestConstraint constraint, @Nonnull OpenQuestAsset asset, @Nullable AbstractQuestProgression<?> quest);

    /**
     * @return {@code true} for a run that ended, which is where a rule on how a quest runs stops
     * being worth a line.
     */
    static boolean isOver(@Nullable AbstractQuestProgression<?> quest) {
        return quest != null && quest.isCompleted();
    }

    /**
     * A label on the left and, when the rule has a figure to give, that figure on the right.
     */
    record Line(@Nonnull Message label, @Nullable Message value) {

        /**
         * @return a line that is only a label, for a rule with no figure to give.
         */
        @Nonnull
        public static Line of(@Nonnull Message label) {
            return new Line(label, null);
        }
    }
}
