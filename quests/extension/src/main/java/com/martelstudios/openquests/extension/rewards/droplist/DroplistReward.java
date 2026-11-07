package com.martelstudios.openquests.extension.rewards.droplist;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemDropList;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsServices;
import com.martelstudios.openquests.core.rewards.QuestReward;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.extension.rewards.item.ItemReward;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rolls one of the game's drop lists {@code Rolls} times. What comes out goes in the player's
 * inventory, the way the {@code Item} reward does, or lands on the ground with
 * {@code DropOnGround}: at the player's feet, or at {@code Position} in the player's world.
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
                                                                         .append(new KeyedCodec<>("DropOnGround", Codec.BOOLEAN), (reward, onGround) -> reward.dropOnGround = onGround, reward -> Boolean.valueOf(reward.dropOnGround))
                                                                         .add()
                                                                         .append(new KeyedCodec<>("Position", Vector3dUtil.CODEC), (reward, position) -> reward.position = position, reward -> reward.position)
                                                                         .add()
                                                                         .build();

    protected String droplist;

    protected int rolls = 1;

    protected boolean dropOnGround;

    /**
     * {@code null} for the player's feet. Only read with {@link #dropOnGround}.
     */
    @Nullable
    protected Vector3d position;

    private DroplistReward() {}

    /**
     * Into the inventory, all rolls go in together or none does: a full inventory leaves the
     * reward owed, and the next attempt rolls anew. On the ground, only a player out of world
     * leaves it owed, having no world to drop it in.
     */
    @Override
    public boolean grant(@Nonnull OpenQuestsServices services, @Nonnull UUID sourceQuestId, @Nonnull EntityComponents playerComponents) {
        if (dropOnGround) return dropOnGround(playerComponents);

        ItemContainer inventory = ItemReward.inventoryOf(playerComponents);
        if (inventory == null) return false;

        List<ItemStack> drops = roll();
        if (drops.isEmpty()) return true;

        return inventory.addItemStacks(drops, true, false, ItemContainer.DEFAULT_FILTER).succeeded();
    }

    private boolean dropOnGround(@Nonnull EntityComponents playerComponents) {
        Ref<EntityStore> reference = playerComponents.getReference();
        if (reference == null || !reference.isValid()) return false;

        List<ItemStack> drops = roll();
        if (drops.isEmpty()) return true;

        Store<EntityStore> store = reference.getStore();
        Runnable spawn = () -> {
            if (!reference.isValid()) return;

            Vector3d at = position;
            if (at == null) {
                TransformComponent transform = store.getComponent(reference, TransformComponent.getComponentType());
                if (transform == null) return;
                at = transform.getPosition();
            }

            store.addEntities(ItemComponent.generateItemDrops(store, drops, new Vector3d(at), Rotation3f.ZERO), AddReason.SPAWN);
        };

        // Entities are only added from their world's thread, where the player stands right now
        if (store.isInThread()) spawn.run();
        else store.getExternalData().getWorld().execute(spawn);

        return true;
    }

    @Nonnull
    private List<ItemStack> roll() {
        List<ItemStack> drops = new ArrayList<>();
        for (int roll = 0; roll < rolls; roll++) {
            drops.addAll(ItemModule.get().getRandomItemDrops(droplist));
        }

        return drops;
    }

    /**
     * @return the id of the drop list rolled.
     */
    @Nonnull
    public String getDroplist() {
        return droplist;
    }

    /**
     * @return how many times the list is rolled, each roll drawn on its own.
     */
    public int getRolls() {
        return rolls;
    }
}
