package com.martelstudios.openquests.extension.quests.item;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestProgression;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Runtime state shared by the quests counting items as they are thrown or picked up. Every such quest
 * sees both moves, whichever it counts: a pickup quest guarding against abuse has to know what the
 * player threw.
 *
 * @param <Q> the concrete quest type extending this class
 */
public abstract class ItemExchangeQuestProgression<Q extends ItemExchangeQuestProgression<Q>> extends QuantityQuestProgression<Q> {

    private static final MapCodec<Integer, HashMap<String, Integer>> COUNTS = new MapCodec<>(Codec.INTEGER, HashMap::new);

    public static final BuilderCodec<ItemExchangeQuestProgression> BASE_CODEC = BuilderCodec.abstractBuilder(ItemExchangeQuestProgression.class, QuantityQuestProgression.BASE_CODEC)
                                                                                            .append(new KeyedCodec<>("Thrown", COUNTS), (quest, counts) -> quest.counters.thrown.putAll(counts), quest -> savedCounts(quest.counters.thrown))
                                                                                            .add()
                                                                                            .append(new KeyedCodec<>("Recovered", COUNTS), (quest, counts) -> quest.counters.recovered.putAll(counts), quest -> savedCounts(quest.counters.recovered))
                                                                                            .add()
                                                                                            .build();

    protected final ExchangeCounters counters = new ExchangeCounters();

    /**
     * @return the items this quest counts.
     */
    @Nonnull
    public abstract QuestItemFilter getItemFilter();

    /**
     * @return whether this quest counts that move at all, the other one only feeding its counters.
     */
    protected abstract boolean counts(@Nonnull ItemExchange exchange);

    @Override
    public ItemExchangeQuestAsset getAsset() {
        return (ItemExchangeQuestAsset) super.getAsset();
    }

    /**
     * @return whether the asset leaves out what a throw and pickup loop brings back.
     */
    public boolean isAntiAbuse() {
        var asset = getAsset();
        return asset != null && asset.isAntiAbuse();
    }

    /**
     * Records a move of items this quest counts, keeping the counters only when guarding against abuse.
     *
     * @return whether anything changed, the count or the counters, which is what is worth saving.
     */
    public boolean exchange(@Nonnull UUID playerId, @Nonnull ItemExchange exchange, int quantity) {
        boolean guarded = isAntiAbuse();

        int fresh = quantity;
        if (guarded && exchange == ItemExchange.THROW) fresh = counters.recordThrow(playerId, quantity);
        if (guarded && exchange == ItemExchange.GROUND_PICKUP) fresh = counters.recordGroundPickup(playerId, quantity);

        int counted = counts(exchange) ? fresh : 0;
        if (counted > 0) setCurrentQuantity(getCurrentQuantity() + counted);

        return counted > 0 || (guarded && exchange != ItemExchange.HARVEST);
    }

    /**
     * Left out of the save once empty, which a quest not guarding against abuse always is.
     */
    @Nullable
    private static HashMap<String, Integer> savedCounts(@Nonnull Map<String, Integer> counts) {
        return counts.isEmpty() ? null : new HashMap<>(counts);
    }
}
