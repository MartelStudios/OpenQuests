package com.martelstudios.opendialogues.characters;

/**
 * Why an NPC stops carrying its character. Chunk unloads are not releases: the NPC comes back.
 */
public enum ReleaseReason {
    /** The NPC died. */
    Death,
    /** The NPC was removed for good without dying, by a command or a despawn. */
    Removed,
    /** A mod or a command took the character back. */
    Revoked
}
