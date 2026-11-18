package com.martelstudios.opendialogues.conditions.impl.world;

import com.martelstudios.opendialogues.conditions.CountBounds;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Counts the blocks of some ids within a sphere, walking block by block only the sections whose
 * palette holds one of them.
 */
public final class BlockScan {

    /**
     * Edge of a chunk section, in blocks.
     */
    public static final int SECTION_SIZE = 32;

    private BlockScan() {}

    /**
     * @param max {@code null} for no upper bound.
     * @return whether between {@code min} and {@code max} blocks of the ids have their centre within the radius.
     */
    public static boolean holds(@Nonnull BlockSource source, @Nonnull Vector3d centre, double radius, @Nonnull IntSet ids, int min, @Nullable Integer max) {
        IntList idList = new IntArrayList(ids);
        double squared = radius * radius;
        int minX = (int) Math.floor(centre.x - radius), maxX = (int) Math.floor(centre.x + radius);
        int minY = (int) Math.floor(centre.y - radius), maxY = (int) Math.floor(centre.y + radius);
        int minZ = (int) Math.floor(centre.z - radius), maxZ = (int) Math.floor(centre.z + radius);
        int count = 0;
        for (int sx = Math.floorDiv(minX, SECTION_SIZE); sx <= Math.floorDiv(maxX, SECTION_SIZE); sx++) {
            for (int sy = Math.floorDiv(minY, SECTION_SIZE); sy <= Math.floorDiv(maxY, SECTION_SIZE); sy++) {
                for (int sz = Math.floorDiv(minZ, SECTION_SIZE); sz <= Math.floorDiv(maxZ, SECTION_SIZE); sz++) {
                    BlockSource.SectionView section = source.section(sx, sy, sz);
                    if (section == null || !section.holdsAny(idList)) continue;
                    for (int x = Math.max(minX, sx * SECTION_SIZE); x <= Math.min(maxX, sx * SECTION_SIZE + SECTION_SIZE - 1); x++) {
                        double dx = x + 0.5 - centre.x;
                        for (int y = Math.max(minY, sy * SECTION_SIZE); y <= Math.min(maxY, sy * SECTION_SIZE + SECTION_SIZE - 1); y++) {
                            double dy = y + 0.5 - centre.y;
                            for (int z = Math.max(minZ, sz * SECTION_SIZE); z <= Math.min(maxZ, sz * SECTION_SIZE + SECTION_SIZE - 1); z++) {
                                double dz = z + 0.5 - centre.z;
                                if (dx * dx + dy * dy + dz * dz > squared || !ids.contains(section.block(x, y, z))) continue;
                                if (NearbyCount.settled(++count, min, max)) return CountBounds.within(count, min, max);
                            }
                        }
                    }
                }
            }
        }
        return CountBounds.within(count, min, max);
    }
}
