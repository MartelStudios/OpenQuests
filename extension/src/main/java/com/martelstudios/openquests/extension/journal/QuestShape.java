package com.martelstudios.openquests.extension.journal;

/**
 * How much of a quest is being shown. The journal draws the same quest three ways and a type is
 * asked which one it is looking at rather than which method it landed in — what changes between
 * them is how much room there is to say something, not what there is to say.
 */
public enum QuestShape {

    /** One line, for a quest listed among another's steps. The detail is one click away. */
    ROW,

    /** The entry in the list: a title that folds open onto what the quest is made of. */
    CARD,

    /** The quest on its own, opened from a link or the trail. Falls back to the card's look. */
    PAGE
}
