package com.martelstudios.opendialogues.map;

import com.martelstudios.opendialogues.characters.MapVisibility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Whether a character shows on a player's map, and with which icon.
 */
public final class CharacterMarkerRule {

    /** Vanilla image, for a character naming none. */
    public static final String DEFAULT_ICON = "Coordinate.png";

    /**
     * A character's marker as one player sees it.
     */
    public record Shown(@Nonnull String image, boolean onCompass) {}

    private CharacterMarkerRule() {}

    /**
     * @return one status out of several mods' answers: the image of the highest priority status
     * having one, on the compass if any is; {@code null} for none.
     */
    @Nullable
    public static CharacterMapStatus merge(@Nonnull Collection<CharacterMapStatus> statuses) {
        if (statuses.isEmpty()) return null;
        CharacterMapStatus imaged = null;
        boolean onCompass = false;
        for (CharacterMapStatus status : statuses) {
            onCompass |= status.onCompass();
            if (status.image() != null && (imaged == null || status.priority() > imaged.priority())) imaged = status;
        }
        return imaged == null ? new CharacterMapStatus(null, 0, onCompass) : new CharacterMapStatus(imaged.image(), imaged.priority(), onCompass);
    }

    /**
     * {@code Never} wins over any status; a status reveals a {@code WhenMet} character not met yet.
     *
     * @return the marker, {@code null} when the character stays off this player's map.
     */
    @Nullable
    public static Shown resolve(@Nonnull MapVisibility visibility, @Nullable String ownIcon, boolean met, @Nullable CharacterMapStatus status) {
        if (visibility == MapVisibility.Never) return null;
        if (visibility == MapVisibility.WhenMet && !met && status == null) return null;
        String image = status != null && status.image() != null ? status.image() : ownIcon != null ? ownIcon : DEFAULT_ICON;
        return new Shown(image, status != null && status.onCompass());
    }
}
