package com.martelstudios.opendialogues.conditions.impl.items;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.ItemResourceType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ResourceType;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * Which items a condition accepts: one item, every item under a tag, every item of a bench category
 * (any rune), or every item of a resource type, the families recipes accept such as any sword or any
 * flower. Same keys as OpenQuests, plus the category.
 */
public class OpenDialoguesItemFilter {

    private static final int NO_TAG = Integer.MIN_VALUE;

    public static final BuilderCodec<OpenDialoguesItemFilter> CODEC = BuilderCodec.builder(OpenDialoguesItemFilter.class, OpenDialoguesItemFilter::new)
                                                                             .append(new KeyedCodec<>("BlockTag", Codec.STRING), (filter, tag) -> filter.blockTag = tag, filter -> filter.blockTag)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("Category", Codec.STRING), (filter, category) -> filter.category = category, filter -> filter.category)
                                                                             .add()
                                                                             .append(new KeyedCodec<>("ItemId", Codec.STRING), (filter, id) -> filter.itemId = id, filter -> filter.itemId)
                                                                             .addValidator(Item.VALIDATOR_CACHE.getValidator())
                                                                             .add()
                                                                             .append(new KeyedCodec<>("ResourceTypeId", Codec.STRING), (filter, id) -> filter.resourceTypeId = id, filter -> filter.resourceTypeId)
                                                                             .addValidator(ResourceType.VALIDATOR_CACHE.getValidator())
                                                                             .add()
                                                                             .validator((filter, results) -> {
                                                                                 if (filter.blockTag == null && filter.category == null && filter.itemId == null && filter.resourceTypeId == null) {
                                                                                     results.fail("One of BlockTag, Category, ItemId or ResourceTypeId must be set");
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
    protected String category;

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

        Item item = Item.getAssetMap().getAsset(id);
        if (category != null && inCategory(item == null ? null : item.getCategories())) return true;
        return resourceTypeId != null && isOfResourceType(item);
    }

    /**
     * @param categories an item's bench categories, {@code null} for none.
     */
    boolean inCategory(@Nullable String[] categories) {
        return categories != null && Arrays.asList(categories).contains(category);
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
