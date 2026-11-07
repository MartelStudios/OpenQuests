package com.martelstudios.openquests.extension.rewards.droplist;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.extension.rewards.item.ItemReward;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rolls one of the game's drop lists {@code Rolls} times and puts what comes out in the player's
 * inventory, the way the {@code Item} reward does.
 */
public class DroplistReward extends QuestReward {

    public static final BuilderCodec<DroplistReward> CODEC = BuilderCodec.builder(DroplistReward.class, DroplistReward::new, QuestReward.BASE_CODEC)
                                                                         .append(new KeyedCodec<>("Droplist", Codec.STRING, true), (reward, droplist) -> reward.droplist = droplist, reward -> reward.droplist)
                                                                         .addValidator(Validators.nonNull())
                                                                         .addValidatorLate(() -> ItemDropList.VALIDATOR_CACHE.getValidator().late())
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Rolls", Codec.INTEGER), (reward, rolls) -> reward.rolls = rolls, reward -> Integer.valueOf(reward.rolls))
                                                                         .addValidator(Validators.greaterThan(0))
                                                                         .add()
                                                                         .build();

    protected String droplist;

    protected int rolls = 1;

    private DroplistReward() {}

    /**
     * All rolls go in together or none does. A full inventory leaves the reward owed, and the next
     * attempt rolls anew: nothing was handed over, so nothing is lost.
     */
    @Override
    public boolean grant(@Nonnull OpenQuestsServices services, @Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        ItemContainer inventory = ItemReward.inventoryOf(playerComponents);
        if (inventory == null) return false;

        List<ItemStack> drops = new ArrayList<>();
        for (int roll = 0; roll < rolls; roll++) {
            drops.addAll(ItemModule.get().getRandomItemDrops(droplist));
        }

        if (drops.isEmpty()) return true;

        return inventory.addItemStacks(drops, true, false, ItemContainer.DEFAULT_FILTER).succeeded();
    }

    /**
     * @return the id of the drop list rolled.
     */
    @Nonnull
    public String getDroplist() {
        return droplist;
    }

    /**
     * @return how many times the list is rolled.
     */
    public int getRolls() {
        return rolls;
    }
}
