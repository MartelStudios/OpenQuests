package com.martelstudios.opendialogues.conditions.impl.items;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.ItemResourceType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ResourceType;

import javax.annotation.Nullable;

/**
 * Which items a condition accepts: one item, every item under a tag, or every item of a resource
 * type, the families recipes accept such as any sword or any flower. Same keys as OpenQuests.
 */
public class OpenDialoguesItemFilter {

    private static final int NO_TAG = Integer.MIN_VALUE;

    public static final BuilderCodec<OpenDialoguesItemFilter> CODEC = BuilderCodec.builder(OpenDialoguesItemFilter.class, OpenDialoguesItemFilter::new)
                                                                             .append(new KeyedCodec<>("BlockTag", Codec.STRING), (filter, tag) -> filter.blockTag = tag, filter -> filter.blockTag)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("ItemId", Codec.STRING), (filter, id) -> filter.itemId = id, filter -> filter.itemId)
                                                                             .addValidator(Item.VALIDATOR_CACHE.getValidator())
                                                                             .add()
                                                                             .append(new KeyedCodec<>("ResourceTypeId", Codec.STRING), (filter, id) -> filter.resourceTypeId = id, filter -> filter.resourceTypeId)
                                                                             .addValidator(ResourceType.VALIDATOR_CACHE.getValidator())
                                                                             .add()
                                                                             .validator((filter, results) -> {
                                                                                 if (filter.blockTag == null && filter.itemId == null && filter.resourceTypeId == null) {
                                                                                     results.fail("One of BlockTag, ItemId or ResourceTypeId must be set");
                                                                                 }
                                                                             })
                                                                             .afterDecode(filter -> {
                                                                                 if (filter.blockTag != null) filter.blockTagIndex = AssetRegistry.getOrCreateTagIndex(filter.blockTag);
                                                                             })
                                                                             .build();

    @Nullable
    protected String blockTag;

    protected int blockTagIndex = NO_TAG;

    @Nullable
    protected String itemId;

    @Nullable
    protected String resourceTypeId;

    protected OpenDialoguesItemFilter() {}

    /**
     * @param id an item id, {@code null} for an empty slot.
     * @return whether any of the keys set accepts it.
     */
    public boolean matches(@Nullable String id) {
        if (id == null) return false;
        if (id.equals(itemId)) return true;
        if (blockTagIndex != NO_TAG && Item.getAssetMap().getKeysForTag(blockTagIndex).contains(id)) return true;

        return resourceTypeId != null && isOfResourceType(Item.getAssetMap().getAsset(id));
    }

    private boolean isOfResourceType(@Nullable Item item) {
        ItemResourceType[] types = item == null ? null : item.getResourceTypes();
        if (types == null) return false;

        for (ItemResourceType type : types) {
            if (resourceTypeId.equals(type.id)) return true;
        }
        return false;
    }
}
