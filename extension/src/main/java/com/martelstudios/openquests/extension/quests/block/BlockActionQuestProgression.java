package com.martelstudios.openquests.extension.quests.block;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.martelstudios.openquests.extension.quests.item.QuestItemFilter;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

/**
 * Runtime state shared by the quests counting blocks placed or broken. Every such quest sees both
 * actions, whichever it counts: a break quest guarding against abuse has to know what was placed.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class BlockActionQuestProgression<Q extends BlockActionQuestProgression<Q>> extends QuantityQuestProgression<Q> {

    public static final BuilderCodec<BlockActionQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(BlockActionQuestProgression.class, QuantityQuestProgression.BASE_CODEC)
                                                                                           .append(new KeyedCodec<>("Placed", new ArrayCodec<>(Codec.STRING, String[]::new)), (quest, positions) -> quest.placed.positions.addAll(Arrays.asList(positions)), quest -> quest.placed.positions.isEmpty() ? null : quest.placed.positions.toArray(String[]::new))
                                                                                           .add()
                                                                                           .append(new KeyedCodec<>("Recovered", new MapCodec<>(Codec.INTEGER, HashMap::new)), (quest, counts) -> quest.placed.recovered.putAll(counts), quest -> quest.placed.recovered.isEmpty() ? null : new HashMap<>(quest.placed.recovered))
                                                                                           .add()
                                                                                           .build();

    protected final PlacedBlocks placed = new PlacedBlocks();

    /**
     * @return the blocks this quest counts.
     */
    @Nonnull
    public abstract QuestItemFilter getBlockFilter();

    /**
     * @return whether this quest counts that action at all, the other one only feeding its memory.
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
     * Records an action on a block this quest counts, remembering placements only when guarding
     * against abuse, and forgetting them once the quest is done counting.
     *
     * @return whether anything changed, the count or the memory, which is what is worth saving.
     */
    public boolean act(@Nonnull UUID playerId, @Nonnull BlockAction action, @Nonnull String position) {
        boolean guarded = isAntiAbuse();

        boolean fresh = true;
        if (guarded && action == BlockAction.PLACE) fresh = placed.recordPlacement(playerId, position);
        if (guarded && action == BlockAction.BREAK) fresh = !placed.recordBreak(playerId, position);

        boolean counted = fresh && counts(action);
        if (counted) setCurrentQuantity(getCurrentQuantity() + 1);

        if (guarded && checkCompletion() && isStopOnComplete()) placed.clear();

        return counted || guarded;
    }
}
