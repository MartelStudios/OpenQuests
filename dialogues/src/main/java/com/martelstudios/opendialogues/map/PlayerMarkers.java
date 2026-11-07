package com.martelstudios.opendialogues.map;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * What the provider keeps of one online player. One object, so leaving drops it in one remove and
 * a recompute finishing late can tell its player was dropped meanwhile.
 */
final class PlayerMarkers {

    /** Which characters the player sees, {@code null} until first decided. */
    volatile CharacterMarkerProvider.Seen seen;

    /** The markers joined from {@link #seen} and the positions, {@code null} until first joined. */
    volatile CharacterMarkerProvider.Built built;

    /** Starts stale, so the first call decides. */
    volatile boolean stale = true;

    volatile boolean mapOpen;

    /** Set while a recompute waits on the world thread, so calls do not queue more. */
    final AtomicBoolean pending = new AtomicBoolean();
}
