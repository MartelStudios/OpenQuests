package com.martelstudios.opendialogues.conditions;

import javax.annotation.Nullable;

/**
 * The {@code Min} and optional {@code Max} bounds, both inclusive, of conditions that count something.
 */
public final class CountBounds {

    private CountBounds() {}

    /**
     * @param max {@code null} for no upper bound.
     * @return whether the count lies between the bounds.
     */
    public static boolean within(int count, int min, @Nullable Integer max) {
        return count >= min && (max == null || count <= max);
    }

    /**
     * @return why no count can lie between the bounds, {@code null} if one can.
     */
    @Nullable
    public static String validate(int min, @Nullable Integer max) {
        return max != null && max < min ? "Max " + max + " is below Min " + min : null;
    }
}
