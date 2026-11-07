package com.martelstudios.opendialogues.map;

import javax.annotation.Nullable;

/**
 * A character's standing for one player, as a {@link CharacterStatusProvider} sees it.
 *
 * @param image     overrides the character's icon, {@code null} to keep it.
 * @param priority  the highest one with an image gives the icon.
 * @param onCompass whether the marker also shows on the compass.
 */
public record CharacterMapStatus(@Nullable String image, int priority, boolean onCompass) {}
