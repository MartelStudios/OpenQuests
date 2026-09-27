package com.martelstudios.openquests.extension.quests.item;

import com.hypixel.hytale.assetstore.AssetRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.ItemResourceType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ResourceType;

import javax.annotation.Nullable;

/**
 * What a quest counts: one item, every item under a tag, or every item of a resource type, the
 * families recipes accept such as any flower or any raw meat. Reads the same keys as the game's own
 * objectives, so a quest written for those reads the same here.
 */
public class QuestItemFilter {

    private static final int NO_TAG = Integer.MIN_VALUE;

    public static final BuilderCodec<QuestItemFilter> CODEC = BuilderCodec.builder(QuestItemFilter.class, QuestItemFilter::new)
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

    protected QuestItemFilter() {}

    /**
     * A block is matched through the item it comes from, which shares its id.
     *
     * @param id an item id or a block type id.
     * @return whether any of the keys set accepts it.
     */
    public boolean matches(@Nullable String id) {
        if (id == null) return false;
        if (id.equals(itemId)) return true;
        if (blockTagIndex != NO_TAG && Item.getAssetMap().getKeysForTag(blockTagIndex).contains(id)) return true;

        return resourceTypeId != null && isOfResourceType(Item.getAssetMap().getAsset(id));
    }

    /**
     * @return the single item counted, {@code null} when the quest counts a family.
     */
    @Nullable
    public String getItemId() {
        return itemId;
    }

    /**
     * @return the resource type counted, {@code null} when the quest names items another way.
     */
    @Nullable
    public String getResourceTypeId() {
        return resourceTypeId;
    }

    /**
     * @return the name a default title gives what is counted: the item's, else the resource type's,
     * else {@code null}, a tag having no name meant for players.
     */
    @Nullable
    public Message getName() {
        Item item = itemId == null ? null : Item.getAssetMap().getAsset(itemId);
        if (item != null) return item.getTranslationMessage();

        if (resourceTypeId == null || ResourceType.getAssetMap().getAsset(resourceTypeId) == null) return null;

        return Message.translation("server.resourceType." + resourceTypeId + ".name");
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
