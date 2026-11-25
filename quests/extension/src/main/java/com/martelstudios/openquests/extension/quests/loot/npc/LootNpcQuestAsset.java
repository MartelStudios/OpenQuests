package com.martelstudios.openquests.extension.quests.loot.npc;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.martelstudios.openquests.extension.quests.quantity.QuantityQuestAsset;

import javax.annotation.Nonnull;

/**
 * Receive a number of one item, dropped only for this quest by NPCs of the listed groups.
 */
public class LootNpcQuestAsset extends QuantityQuestAsset {

    public static final BuilderCodec<LootNpcQuestAsset> CODEC =
        BuilderCodec.builder(LootNpcQuestAsset.class, LootNpcQuestAsset::new, QuantityQuestAsset.BASE_CODEC)
                    .append(new KeyedCodec<>("ItemId", Codec.STRING, true), (asset, itemId) -> asset.itemId = itemId, asset -> asset.itemId)
                    .addValidator(Validators.nonNull())
                    .addValidator(Item.VALIDATOR_CACHE.getValidator())
                    .add()
                    .append(new KeyedCodec<>("Drops", new ArrayCodec<>(LootNpcDrop.CODEC, LootNpcDrop[]::new), true), (asset, drops) -> asset.drops = drops, asset -> asset.drops)
                    .addValidator(Validators.nonEmptyArray())
                    .addValidator(Validators.nonNullArrayElements())
                    .add()
                    .build();

    protected String itemId;

    protected LootNpcDrop[] drops;

    private LootNpcQuestAsset() {}

    @Override
    public LootNpcQuestProgression.Builder builder() {
        return new LootNpcQuestProgression.Builder(this);
    }

    /**
     * @return the item dropped and counted, a concrete one since a filter cannot be spawned.
     */
    @Nonnull
    public String getItemId() {
        return itemId;
    }

    /**
     * @return the sources of the item, at least one.
     */
    @Nonnull
    public LootNpcDrop[] getDrops() {
        return drops;
    }
}
