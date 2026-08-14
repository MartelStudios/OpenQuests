package com.martelstudios.hyquests.extension.rewards;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.core.assets.QuestAsset;
import com.martelstudios.hyquests.core.rewards.QuestReward;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Gives items to the player, hotbar first then storage. The item id is validated at asset load,
 * so a typo fails the boot rather than the reward.
 */
public class ItemQuestReward extends QuestReward {

    public static final BuilderCodec<ItemQuestReward> CODEC = BuilderCodec.builder(ItemQuestReward.class, ItemQuestReward::new)
                                                                          .append(new KeyedCodec<>("ItemId", Codec.STRING), (reward, itemId) -> reward.itemId = itemId, reward -> reward.itemId)
                                                                          .addValidator(Item.VALIDATOR_CACHE.getValidator())
                                                                          .add()
                                                                          .append(new KeyedCodec<>("Quantity", Codec.INTEGER), (reward, quantity) -> reward.quantity = quantity, reward -> Integer.valueOf(reward.quantity))
                                                                          .add()
                                                                          .build();

    protected String itemId;
    protected int quantity = 1;

    private ItemQuestReward() {}

    @Override
    public void grant(@Nonnull QuestAsset questAsset, @Nonnull Ref<EntityStore> playerRef) {
        List<ItemContainer> containers = new ArrayList<>(InventoryComponent.HOTBAR_FIRST.length);

        for (var inventoryType : InventoryComponent.HOTBAR_FIRST) {
            var inventory = playerRef.getStore().getComponent(playerRef, inventoryType);
            if (inventory != null) containers.add(inventory.getInventory());
        }

        if (containers.isEmpty()) return;

        new CombinedItemContainer(containers.toArray(new ItemContainer[0])).addItemStack(new ItemStack(itemId, quantity));
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
