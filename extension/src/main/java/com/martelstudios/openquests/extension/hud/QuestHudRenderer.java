package com.martelstudios.openquests.extension.hud;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;

/**
 * How one quest type draws itself in the tracker. The panel picks which quests get in and stops at
 * five; everything past that — lines, colours, progress — belongs here.
 */
public interface QuestHudRenderer {

    /**
     * @return the quest class this renders. Subclasses fall back to it, so registering on a base
     * type covers every type built on it.
     */
    @Nonnull
    Class<?> getQuestType();

    /**
     * Ids of the quests this one draws itself. They are kept out of the panel, since a quest shown
     * under its parent has no reason to appear twice.
     */
    @Nonnull
    default Set<UUID> getOwnedQuestIds(@Nonnull AbstractQuestProgression<?> quest) {
        return Set.of();
    }

    /**
     * Draws the quest, as many lines as it takes. {@link QuestHudRows} holds the plain look for
     * whoever only wants to add to it.
     *
     * @param indented whether another quest is drawing this one under itself. Forward it to
     *                 {@link QuestHudRows}, which picks the line accordingly.
     */
    void render(@Nonnull QuestHudContext context, @Nonnull AbstractQuestProgression<?> quest, boolean indented);
}
