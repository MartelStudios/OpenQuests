package com.martelstudios.opendialogues.characters;

/**
 * When the NPCs carrying a character appear on a player's world map.
 */
public enum MapVisibility {
    /** Never, even when a quest leads to it. */
    Never,
    /** Once the player talked to the character, or while a running quest leads to it. */
    WhenMet,
    /** As soon as an NPC carries the character. */
    Always
}
