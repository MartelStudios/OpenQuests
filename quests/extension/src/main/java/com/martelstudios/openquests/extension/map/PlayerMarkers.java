package com.martelstudios.openquests.extension.map;

/**
 * What the provider keeps of one online player. One object, so leaving drops it in one remove.
 */
final class PlayerMarkers {

    /** {@code null} until first computed. */
    volatile QuestTargetMarkerProvider.Built built;

    volatile boolean stale;

    volatile boolean mapOpen;
}
