package com.martelstudios.openquests.extension.journal;

import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestAsset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * How one quest type shows itself in the journal. The page owns the frame of each shape — the
 * title, the fold, the rewards, the buttons — and hands the inside over here, so a new quest type
 * explains itself without the page knowing it exists.
 *
 * <p>Both halves are optional, and they answer different needs. {@link #documentFor} changes what
 * a shape <em>looks</em> like and asks for no code; {@link #render} fills it, and is only needed by
 * a type whose lines cannot be written down in advance — a chain has as many as it has steps.
 */
public interface QuestPageRenderer {

    /**
     * @return the quest class this renders. Subclasses fall back to it, so registering on a base
     * type covers every type built on it.
     */
    @Nonnull
    Class<?> getQuestType();

    /**
     * @return the document the journal appends for that shape, or {@code null} to take its own. A
     * replacement carries the names the journal writes into — {@code #Title}, {@code #Status},
     * {@code #Progress}, {@code #Details}, {@code #Objectives} — and whatever else it likes; a name
     * it leaves out is simply never written.
     *
     * <p>Only asked for {@link QuestShape#CARD} and {@link QuestShape#PAGE}, the shapes whose
     * container the journal opens. A row is a line the renderer appends itself, so a type wanting
     * another one appends another document in {@link #render}.
     */
    @Nullable
    default String documentFor(@Nonnull QuestShape shape) {
        return null;
    }

    /**
     * Writes what this type has to say into the shape being drawn. Appending nothing is the right
     * answer for most types and every shape: a quest asking for one thing already said which in
     * its title, and repeating it under a heading says it twice.
     *
     * <p>A row is expected to append exactly one line, since it stands for the quest in a list.
     * A card and a page write onto the row through {@code selector} — a counter beside the title —
     * and append their objectives, which land under the heading wherever they are appended from.
     *
     * @param quest the progression, or {@code null} for a quest the journal cannot place: one the
     * player was never handed, or one that ended and kept no trace of itself.
     * @param asset what the quest was built from, the only thing left to read when there is no
     * progression. Null only when that is gone too, which leaves nothing to say at all.
     */
    default void render(@Nonnull QuestPageContext context, @Nonnull QuestShape shape, @Nonnull String selector,
                        @Nullable AbstractQuestProgression<?> quest, @Nullable QuestAsset asset) {}

}
