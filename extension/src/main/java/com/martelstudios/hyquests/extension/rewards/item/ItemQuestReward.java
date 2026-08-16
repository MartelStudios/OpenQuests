package com.martelstudios.hyquests.extension.rewards.item;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.martelstudios.hyquests.core.history.models.QuestHistoryRecord;
import com.martelstudios.hyquests.core.rewards.QuestReward;
import com.martelstudios.hyquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

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
    public boolean grant(@Nonnull QuestHistoryRecord questHistoryRecord, @Nonnull EntityComponents playerComponents) {
        List<ItemContainer> containers = new ArrayList<>(InventoryComponent.HOTBAR_FIRST.length);

        for (var inventoryType : InventoryComponent.HOTBAR_FIRST) {
            var inventory = playerComponents.getComponent(inventoryType);
            if (inventory != null) containers.add(inventory.getInventory());
        }

        if (containers.isEmpty()) return false;

        var combined = new CombinedItemContainer(containers.toArray(new ItemContainer[0]));

        // Add with allOrNothing
        var transaction = combined.addItemStack(new ItemStack(itemId, quantity), true, false, ItemContainer.DEFAULT_FILTER);

        return transaction.succeeded();
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
