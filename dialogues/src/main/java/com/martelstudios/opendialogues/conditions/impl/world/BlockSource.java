package com.martelstudios.opendialogues.conditions.impl.world;

import it.unimi.dsi.fastutil.ints.IntList;

import javax.annotation.Nullable;

/**
 * The blocks {@link BlockScan} reads, one section of {@link BlockScan#SECTION_SIZE} blocks at a time,
 * so the scan can be tested without a world.
 */
public interface BlockSource {

    /**
     * @return the section at these section coordinates, {@code null} when not in memory or outside the world height.
     */
    @Nullable
    SectionView section(int sx, int sy, int sz);

    /**
     * One loaded section.
     */
    interface SectionView {

        /**
         * @return whether the section's palette holds one of the ids, without reading its blocks.
         */
        boolean holdsAny(IntList ids);

        /**
         * @return the block id at these world coordinates, which lie in this section.
         */
        int block(int x, int y, int z);
    }
}
