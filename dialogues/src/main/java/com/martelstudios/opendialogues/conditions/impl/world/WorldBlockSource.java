package com.martelstudios.opendialogues.conditions.impl.world;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.accessor.SectionReader;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import it.unimi.dsi.fastutil.ints.IntList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A world's blocks as {@link BlockScan} reads them, only from sections already in memory: a scan
 * never loads nor generates a chunk. Read on the world's thread.
 */
public final class WorldBlockSource implements BlockSource {

    private final ChunkStore chunks;
    private final SectionReader reader;

    /**
     * @param chunks the chunk store of the player's world.
     */
    public WorldBlockSource(@Nonnull ChunkStore chunks) {
        this.chunks = chunks;
        this.reader = new SectionReader(chunks);
    }

    @Nullable
    @Override
    public SectionView section(int sx, int sy, int sz) {
        int x = sx * BlockScan.SECTION_SIZE, y = sy * BlockScan.SECTION_SIZE, z = sz * BlockScan.SECTION_SIZE;
        if (!reader.canHoldY(y) || !reader.hasStorageInMemory(x, y, z)) return null;
        Ref<ChunkStore> ref = reader.sectionAt(x, y, z);
        BlockSection section = ref == null || !ref.isValid() ? null : chunks.getStore().getComponent(ref, BlockSection.getComponentType());
        if (section == null) return null;
        return new SectionView() {
            @Override
            public boolean holdsAny(IntList ids) {
                return section.containsAny(ids);
            }

            @Override
            public int block(int bx, int by, int bz) {
                return reader.getBlock(bx, by, bz);
            }
        };
    }
}
