package com.martelstudios.opendialogues.conditions.impl.world;

import it.unimi.dsi.fastutil.ints.IntList;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockScanTest {

    private static final int MUSHROOM = 7;
    private static final IntList IDS = IntList.of(MUSHROOM);
    private static final Vector3d CENTRE = new Vector3d(0.5, 64.5, 0.5);

    @Test
    void countsOnlyInsideTheSphere() {
        FakeBlocks blocks = new FakeBlocks().put(9, 64, 0, MUSHROOM).put(11, 64, 0, MUSHROOM);

        assertFalse(BlockScan.holds(blocks, CENTRE, 10, IDS, 2, null));
        assertTrue(BlockScan.holds(blocks, CENTRE, 10, IDS, 1, null));
    }

    @Test
    void readsAcrossSections() {
        FakeBlocks blocks = new FakeBlocks().put(-3, 64, 0, MUSHROOM).put(3, 64, 0, MUSHROOM);

        assertTrue(BlockScan.holds(blocks, CENTRE, 10, IDS, 2, null));
    }

    @Test
    void skipsSectionsWithoutTheIds() {
        FakeBlocks blocks = new FakeBlocks().put(2, 64, 0, 3);

        assertFalse(BlockScan.holds(blocks, CENTRE, 10, IDS, 1, null));
        assertEquals(0, blocks.reads.get());
    }

    @Test
    void unloadedSectionCountsNothing() {
        FakeBlocks blocks = new FakeBlocks().put(2, 64, 0, MUSHROOM);
        blocks.unloaded.add(List.of(0, 2, 0));

        assertFalse(BlockScan.holds(blocks, CENTRE, 10, IDS, 1, null));
    }

    @Test
    void maxZeroStopsAtFirstMatch() {
        FakeBlocks blocks = new FakeBlocks().put(1, 64, 0, MUSHROOM).put(2, 64, 0, MUSHROOM);

        assertFalse(BlockScan.holds(blocks, CENTRE, 10, IDS, 0, 0));
        assertTrue(BlockScan.holds(new FakeBlocks(), CENTRE, 10, IDS, 0, 0));
    }

    @Test
    void centreNearWorldBottom() {
        FakeBlocks blocks = new FakeBlocks().put(0, 0, 0, MUSHROOM);

        assertTrue(BlockScan.holds(blocks, new Vector3d(0.5, 2, 0.5), 10, IDS, 1, null));
    }

    /**
     * Blocks by world position; sections below y 0 are outside the world and give no view.
     */
    private static final class FakeBlocks implements BlockSource {

        final Map<List<Integer>, Integer> blocks = new HashMap<>();
        final Set<List<Integer>> unloaded = new HashSet<>();
        final AtomicInteger reads = new AtomicInteger();

        FakeBlocks put(int x, int y, int z, int id) {
            blocks.put(List.of(x, y, z), id);
            return this;
        }

        @Nullable
        @Override
        public SectionView section(int sx, int sy, int sz) {
            if (sy < 0 || unloaded.contains(List.of(sx, sy, sz))) return null;
            return new SectionView() {
                @Override
                public boolean holdsAny(IntList ids) {
                    return blocks.entrySet().stream().anyMatch(entry -> sectionOf(entry.getKey(), sx, sy, sz) && ids.contains(entry.getValue().intValue()));
                }

                @Override
                public int block(int x, int y, int z) {
                    reads.incrementAndGet();
                    return blocks.getOrDefault(List.of(x, y, z), 0);
                }
            };
        }

        private static boolean sectionOf(List<Integer> position, int sx, int sy, int sz) {
            return Math.floorDiv(position.get(0), BlockScan.SECTION_SIZE) == sx
                && Math.floorDiv(position.get(1), BlockScan.SECTION_SIZE) == sy
                && Math.floorDiv(position.get(2), BlockScan.SECTION_SIZE) == sz;
        }
    }
}
