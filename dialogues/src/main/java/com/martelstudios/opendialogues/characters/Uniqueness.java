package com.martelstudios.opendialogues.characters;

/**
 * How many NPCs of one world, or of the universe with {@link CharacterScope#Universe}, may carry a character.
 */
public enum Uniqueness {
    /** Any number, each drawn on its own. */
    None,
    /** One at a time: free to draw again once its NPC is released. */
    Alive,
    /** One at a time, and lost for that world once its NPC dies. */
    Forever;

    /**
     * @return whether carrying the character takes the world's only slot.
     */
    public boolean claims() {
        return this != None;
    }

    /**
     * @return whether releasing the carrier for that reason loses the character for good.
     */
    public boolean consumedBy(ReleaseReason reason) {
        return this == Forever && reason == ReleaseReason.Death;
    }
}
