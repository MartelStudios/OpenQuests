package com.martelstudios.openquests.extension.journal;

import com.martelstudios.openquests.core.models.QuestState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * What the journal is able to say about a quest, which is a little more than what a quest can be:
 * one the player was never handed has no state at all, and saying so is the whole point of showing
 * it. Kept here rather than added to {@link QuestState}, since being locked is a fact about the
 * player rather than about the quest.
 *
 * <p>Saying nothing is {@code null} wherever a mark is passed around, and that is not the same as
 * {@link #LOCKED}: a branch of an OR group that ended is one the player may well have walked, and
 * marking it shut would be a guess.
 */
public enum QuestMark {
    IN_PROGRESS(QuestState.IN_PROGRESS),
    SUCCESSFUL(QuestState.SUCCESSFUL),
    FAILED(QuestState.FAILED),
    ABANDONED(QuestState.ABANDONED),

    /** Never handed out, so there is nothing to have become of it yet. */
    LOCKED(null),

    /**
     * Handed out once and gone since: the id something still points at leads nowhere, because the
     * quest was told to keep no trace of itself, or because whatever held it is no longer there.
     *
     * <p>Its own mark rather than a guess at the likeliest outcome. A step missing from a chain
     * used to be read as one that succeeded, which is the one thing a journal must not do — say in
     * green something it does not know.
     */
    LOST(null);

    @Nullable
    private final QuestState state;

    QuestMark(@Nullable QuestState state) {
        this.state = state;
    }

    @Nonnull
    public static QuestMark of(@Nonnull QuestState state) {
        return switch (state) {
            case SUCCESSFUL -> SUCCESSFUL;
            case FAILED -> FAILED;
            case ABANDONED -> ABANDONED;
            default -> IN_PROGRESS;
        };
    }

    /**
     * @param name what {@link #name()} wrote, from a click the page sent itself.
     * @return the mark it names, or {@code null} for anything this version never wrote — a stale
     * client rather than a fault.
     */
    @Nullable
    public static QuestMark parse(@Nullable String name) {
        if (name == null) return null;

        for (QuestMark mark : values()) {
            if (mark.name().equals(name)) return mark;
        }
        return null;
    }

    /**
     * @return the quest state this stands for, or {@code null} for a quest that has reached none.
     */
    @Nullable
    public QuestState toState() {
        return state;
    }

    /**
     * @return how the row names this beside the title. Every mark has a word of its own, locked
     * included, which is what a player reads before the icon means anything to them.
     */
    @Nonnull
    public String getStatusKey() {
        return "openquests.page.status." + name().toLowerCase();
    }
}
