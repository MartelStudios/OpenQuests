package com.martelstudios.openquests.extension.quests.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * The blocks of one chunk section a player placed, one bit per block, saved with the chunk. The
 * world remembers them rather than a quest, so a block placed before the quest was handed out, or
 * by someone outside it, is recognised all the same. Only sections a player built in carry one.
 */
public final class PlacedBlockMarks implements Component<ChunkStore> {

    public static final String REGISTRY_ID = "OpenQuestsPlacedBlocks";

    private static final byte SPARSE = 0;
    private static final byte DENSE = 1;

    public static final BuilderCodec<PlacedBlockMarks> CODEC = BuilderCodec.builder(PlacedBlockMarks.class, PlacedBlockMarks::new)
                                                                           .append(new KeyedCodec<>("Data", Codec.BYTE_ARRAY), PlacedBlockMarks::read, PlacedBlockMarks::write)
                                                                           .add()
                                                                           .build();

    @Nullable
    private static ComponentType<ChunkStore, PlacedBlockMarks> type;

    private final BitSet marked = new BitSet(ChunkUtil.SIZE_BLOCKS);

    /**
     * Registers the component on the chunks, which is what saves the marks with them.
     */
    public static void register(@Nonnull ComponentRegistryProxy<ChunkStore> registry) {
        type = registry.registerComponent(PlacedBlockMarks.class, REGISTRY_ID, CODEC);
    }

    /**
     * Remembers that a player placed the block at these world coordinates.
     */
    public static void mark(@Nonnull World world, int x, int y, int z) {
        ChunkStore chunks = world.getChunkStore();
        Ref<ChunkStore> section = chunks.getChunkSectionReferenceAtBlock(x, y, z);
        if (type == null || section == null || !section.isValid()) return;

        chunks.getStore().ensureAndGetComponent(section, type).mark(ChunkUtil.indexBlock(x, y, z));
    }

    /**
     * Forgets the block at these world coordinates, which is being broken.
     *
     * @return whether a player had placed it.
     */
    public static boolean consume(@Nonnull World world, int x, int y, int z) {
        ChunkStore chunks = world.getChunkStore();
        Ref<ChunkStore> section = chunks.getChunkSectionReferenceAtBlock(x, y, z);
        if (type == null || section == null || !section.isValid()) return false;

        PlacedBlockMarks marks = chunks.getStore().getComponent(section, type);
        if (marks == null || !marks.unmark(ChunkUtil.indexBlock(x, y, z))) return false;

        // A section nobody built in any more saves nothing
        if (marks.isEmpty()) chunks.getStore().tryRemoveComponent(section, type);
        return true;
    }

    void mark(int index) {
        marked.set(index);
    }

    /**
     * @return whether the block was marked.
     */
    boolean unmark(int index) {
        if (!marked.get(index)) return false;

        marked.clear(index);
        return true;
    }

    boolean isMarked(int index) {
        return marked.get(index);
    }

    boolean isEmpty() {
        return marked.isEmpty();
    }

    /**
     * A section with a few blocks placed saves their indices, two bytes each; a busier one saves the
     * bits themselves, whichever is shorter.
     */
    @Nonnull
    private byte[] write() {
        int count = marked.cardinality();
        byte[] bits = marked.toByteArray();

        if (count * Short.BYTES >= bits.length) {
            ByteBuffer dense = ByteBuffer.allocate(1 + bits.length).put(DENSE).put(bits);
            return dense.array();
        }

        ByteBuffer sparse = ByteBuffer.allocate(1 + count * Short.BYTES).put(SPARSE);
        for (int index = marked.nextSetBit(0); index >= 0; index = marked.nextSetBit(index + 1)) {
            sparse.putShort((short) index);
        }
        return sparse.array();
    }

    private void read(@Nullable byte[] data) {
        marked.clear();
        if (data == null || data.length == 0) return;

        ByteBuffer buffer = ByteBuffer.wrap(data, 1, data.length - 1);
        if (data[0] == DENSE) {
            marked.or(BitSet.valueOf(buffer));
            return;
        }

        while (buffer.remaining() >= Short.BYTES) {
            marked.set(Short.toUnsignedInt(buffer.getShort()));
        }
    }

    @Nonnull
    @Override
    public Component<ChunkStore> clone() {
        PlacedBlockMarks copy = new PlacedBlockMarks();
        copy.marked.or(marked);
        return copy;
    }
}
