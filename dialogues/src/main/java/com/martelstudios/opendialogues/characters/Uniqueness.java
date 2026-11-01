package com.martelstudios.opendialogues.characters;

/**
 * How many NPCs of one world may carry a character.
 */
public enum Uniqueness {
    /** Any number, each drawn on its own. */
    None,
    /** One at a time: free to draw again once its NPC dies. */
    Alive,
    /** One ever: lost for that world once its NPC dies. */
    Forever
}
