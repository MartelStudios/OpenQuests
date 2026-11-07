package com.martelstudios.openquests.extension.rewards.item;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gives items to the player, hotbar first then storage. The item id is validated at asset load,
 * so a typo fails the boot rather than the reward.
 */
public class ItemReward extends QuestReward {

    public static final BuilderCodec<ItemReward> CODEC = BuilderCodec.builder(ItemReward.class, ItemReward::new, QuestReward.BASE_CODEC)
                                                                     .append(new KeyedCodec<>("ItemId", Codec.STRING), (reward, itemId) -> reward.itemId = itemId, reward -> reward.itemId)
                                                                     .addValidator(Item.VALIDATOR_CACHE.getValidator())
                                                                     .add()
                                                                     .append(new KeyedCodec<>("Quantity", Codec.INTEGER), (reward, quantity) -> reward.quantity = quantity, reward -> Integer.valueOf(reward.quantity))
                                                                     .add()
                                                                     .build();

    protected String itemId;
    protected int quantity = 1;

    private ItemReward() {}

    @Override
    public boolean grant(@Nonnull OpenQuestsServices services, @Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        ItemContainer inventory = inventoryOf(playerComponents);
        if (inventory == null) return false;

        // Add with allOrNothing
        var transaction = inventory.addItemStack(new ItemStack(itemId, quantity), true, false, ItemContainer.DEFAULT_FILTER);

        return transaction.succeeded();
    }

    /**
     * The player's inventory as one container, hotbar first, for any reward that hands items over.
     *
     * @return {@code null} if the player carries no inventory at all.
     */
    @Nullable
    public static ItemContainer inventoryOf(@Nonnull EntityComponents playerComponents) {
        List<ItemContainer> containers = new ArrayList<>(InventoryComponent.HOTBAR_FIRST.length);

        for (var inventoryType : InventoryComponent.HOTBAR_FIRST) {
            var inventory = playerComponents.getComponent(inventoryType);
            if (inventory != null) containers.add(inventory.getInventory());
        }

        if (containers.isEmpty()) return null;

        return new CombinedItemContainer(containers.toArray(new ItemContainer[0]));
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }
}
