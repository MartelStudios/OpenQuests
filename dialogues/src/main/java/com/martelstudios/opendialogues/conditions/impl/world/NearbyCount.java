package com.martelstudios.opendialogues.conditions.impl.world;

import com.martelstudios.opendialogues.conditions.CountBounds;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Counts what lies near the player for the near conditions, stopping once the answer is known.
 */
public final class NearbyCount {

    /**
     * The largest radius a near condition accepts: the block scan grows with its cube.
     */
    public static final double MAX_RADIUS = 128.0;

    private NearbyCount() {}

    /**
     * @param max {@code null} for no upper bound.
     * @return whether between {@code min} and {@code max} candidates pass, reading no more than needed.
     */
    public static <T> boolean holds(@Nonnull Iterable<T> candidates, @Nonnull Predicate<T> counts, int min, @Nullable Integer max) {
        int count = 0;
        for (T candidate : candidates) {
            if (!counts.test(candidate)) continue;
            count++;
            if (CountBounds.settled(count, min, max)) break;
        }
        return CountBounds.within(count, min, max);
    }

    /**
     * @return why the radius cannot be used, {@code null} if it can.
     */
    @Nullable
    public static String validateRadius(double radius) {
        return radius > 0 && radius <= MAX_RADIUS ? null : "Radius " + radius + " must be above 0 and at most " + MAX_RADIUS;
    }
}
