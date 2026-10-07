package com.martelstudios.openquests.extension.quests.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.UUID;

/**
 * Runtime state shared by the quests counting blocks placed or broken. Every such quest sees both
 * actions, whichever it counts: a place quest guarding against abuse has to know what was broken.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class BlockActionQuestProgression<Q extends BlockActionQuestProgression<Q>> extends QuantityQuestProgression<Q> {

    public static final BuilderCodec<BlockActionQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(BlockActionQuestProgression.class, QuantityQuestProgression.BASE_CODEC)
                                                                                           .append(new KeyedCodec<>("Recovered", new MapCodec<>(Codec.INTEGER, HashMap::new)), (quest, counts) -> quest.recoveries.recovered.putAll(counts), quest -> quest.recoveries.recovered.isEmpty() ? null : new HashMap<>(quest.recoveries.recovered))
                                                                                           .add()
                                                                                           .build();

    protected BlockRecoveries recoveries = new BlockRecoveries();

    /**
     * @return the blocks this quest counts.
     */
    @Nonnull
    public abstract QuestItemFilter getBlockFilter();

    /**
     * @return whether this quest counts that action at all, the other one only feeding its counters.
     */
    protected abstract boolean counts(@Nonnull BlockAction action);

    @Override
    public BlockActionQuestAsset getAsset() {
        return (BlockActionQuestAsset) super.getAsset();
    }

    /**
     * @return whether the asset leaves out a block placed and broken over and over.
     */
    public boolean isAntiAbuse() {
        var asset = getAsset();
        return asset != null && asset.isAntiAbuse();
    }

    /**
     * Records an action on a block this quest counts. Guarding against abuse, a block a player placed
     * does not count once broken, and placing it again does not count either.
     *
     * @param placedByPlayer for a break, whether a player had placed the block, as the world remembers.
     * @return whether anything changed, the count or the counters, which is what is worth saving.
     */
    public boolean act(@Nonnull UUID playerId, @Nonnull BlockAction action, boolean placedByPlayer) {
        boolean guarded = isAntiAbuse();
        boolean changed = false;
        boolean fresh = true;

        if (guarded && action == BlockAction.BREAK && placedByPlayer) {
            fresh = false;
            if (counts(BlockAction.PLACE)) {
                recoveries.record(playerId);
                changed = true;
            }
        }

        if (guarded && action == BlockAction.PLACE && recoveries.consume(playerId)) {
            fresh = false;
            changed = true;
        }

        if (fresh && counts(action)) {
            addQuantity(1);
            changed = true;
        }

        if (guarded && checkCompletion() && isStopOnComplete()) recoveries.clear();

        return changed;
    }
}
