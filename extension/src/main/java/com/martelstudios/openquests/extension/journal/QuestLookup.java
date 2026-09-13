package com.martelstudios.openquests.extension.journal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * What the journal knows about a quest a renderer only has the name of. A renderer listing another
 * quest holds an id and nothing else — the quest it names may be running, finished, or not yet
 * handed out — and deciding which is the page's business rather than its own.
 */
public interface QuestLookup {

    /**
     * @param target a quest id or an asset id, the way the quest commands take either
     * @return whether the journal has a page to show for it. A link that goes nowhere reads as a
     * broken one, and an id naming no asset at all is the case that gets here.
     */
    boolean canOpen(@Nonnull String target);

    /**
     * @return what the journal can say became of it on its own — a progression it still holds, or
     * a completion it kept a record of — and {@code null} when it knows nothing, which leaves
     * whoever is drawing the line free to say what the quest above it was worth.
     */
    @Nullable
    QuestMark markOf(@Nonnull String target);
}
