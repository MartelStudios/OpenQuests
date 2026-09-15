package com.martelstudios.openquests.extension.hud;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;

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
     * Draws the quest, as many lines as it takes. Where those lines end up is the context's
     * business, so the same renderer serves a quest at the top of the panel and one listed under
     * another. {@link QuestHudRows} holds the plain look for whoever only wants to add to it.
     */
    void render(@Nonnull QuestHudContext context, @Nonnull AbstractQuestProgression<?> quest);
}
