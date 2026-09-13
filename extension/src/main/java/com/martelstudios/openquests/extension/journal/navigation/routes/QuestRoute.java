package com.martelstudios.openquests.extension.journal.navigation.routes;

import com.hypixel.hytale.server.core.Message;
import com.martelstudios.opennavigation.routes.StackRoute;
import com.martelstudios.openquests.extension.journal.QuestMark;
import com.martelstudios.openquests.extension.journal.navigation.JournalRoutes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * One quest, opened from the journal or from another quest that named it. The target is part of the
 * name rather than only of the context: two quests are two places, and coming back to one has to
 * land on the right one.
 */
public class QuestRoute extends StackRoute implements LabelledRoute {

    private final String target;

    private final Message label;

    @Nullable
    private final QuestMark mark;

    /**
     * @param target a quest id or an asset id, the way the quest commands take either. An asset is
     * what a quest the player was never given is known by, and it is the only name a step that
     * ended and left the store still answers to.
     * @param label how the trail names it, taken when the route is built. Read once here rather
     * than looked up on every draw, since a quest that ends leaves the store and would stop being
     * nameable halfway up the trail.
     * @param mark what the line that led here said the quest was worth, or {@code null} when it
     * said nothing. Only ever a fallback: a quest still readable is read, and this is what is left
     * for one that ended without keeping a record of its own.
     */
    public QuestRoute(@Nonnull String target, @Nonnull Message label, @Nullable QuestMark mark) {
        super(JournalRoutes.NAMESPACE, JournalRoutes.QUEST + ":" + target);

        this.target = target;
        this.label = label;
        this.mark = mark;
    }

    /**
     * @return the quest id or asset id this route stands for.
     */
    @Nonnull
    public String getTarget() {
        return target;
    }

    @Nullable
    public QuestMark getMark() {
        return mark;
    }

    @Nonnull
    @Override
    public Message getLabel() {
        return label;
    }
}
