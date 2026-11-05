package com.martelstudios.opendialogues.characters.filters;

import com.hypixel.hytale.math.shape.Box;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Whether blocks bury the upper half of an NPC's hitbox. The client aims F at the first hitbox
 * the crosshair meets, so tall grass around a small NPC takes the prompt away from it.
 */
public final class HitboxClearance {

    /**
     * Ignores boxes that only touch, as a block resting at the NPC's feet does.
     */
    private static final double EPSILON = 1.0E-4;

    /**
     * The hitboxes of the block in one cell.
     */
    @FunctionalInterface
    public interface BlockBoxes {

        /**
         * @return the block's hitboxes relative to its cell corner, {@code null} for an empty cell.
         */
        @Nullable
        Box[] at(int x, int y, int z);
    }

    private HitboxClearance() {}

    /**
     * @param x   the NPC's position.
     * @param box the NPC's hitbox, relative to its position.
     * @return whether no block hitbox overlaps the upper half of the NPC's hitbox.
     */
    public static boolean clear(double x, double y, double z, @Nonnull Box box, @Nonnull BlockBoxes blocks) {
        double minX = x + box.min.x, maxX = x + box.max.x;
        double minY = y + (box.min.y + box.max.y) / 2, maxY = y + box.max.y;
        double minZ = z + box.min.z, maxZ = z + box.max.z;
        for (int bx = (int) Math.floor(minX); bx <= (int) Math.floor(maxX); bx++) {
            for (int by = (int) Math.floor(minY); by <= (int) Math.floor(maxY); by++) {
                for (int bz = (int) Math.floor(minZ); bz <= (int) Math.floor(maxZ); bz++) {
                    Box[] boxes = blocks.at(bx, by, bz);
                    if (boxes == null) continue;
                    for (Box block : boxes) {
                        if (overlaps(minX, maxX, bx + block.min.x, bx + block.max.x)
                            && overlaps(minY, maxY, by + block.min.y, by + block.max.y)
                            && overlaps(minZ, maxZ, bz + block.min.z, bz + block.max.z)) return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean overlaps(double minA, double maxA, double minB, double maxB) {
        return minA < maxB - EPSILON && minB < maxA - EPSILON;
    }
}
