package com.martelstudios.openquests.extension.quests.pickupitem;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.event.events.ecs.InteractivelyPickupItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.extension.quests.item.ItemExchange;
import com.martelstudios.openquests.extension.quests.item.ItemExchangeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Tells a pickup apart, which the game raises no event for. An item taken off the ground is removed
 * marked as taken by a player, then a copy flying to that player appears: the removal knows how many
 * there were, the copy knows who took them, and the two follow each other on the world's thread in
 * the same tick. A harvest by hand has an event of its own, which says both.
 */
public final class PickupItemSystems {

    /**
     * The last stack taken off the ground on this world's thread, waiting for the copy that names its
     * taker. Each world ticks on a thread of its own, which is what keeps two worlds apart.
     */
    private final ThreadLocal<TakenStack> taken = new ThreadLocal<>();

    /**
     * @return the system noting what a player took off the ground.
     */
    @Nonnull
    public RefSystem<EntityStore> leftGroundSystem() {
        return new LeftGroundSystem();
    }

    /**
     * @return the system counting it once the copy flying to the player appears.
     */
    @Nonnull
    public RefSystem<EntityStore> flyingItemSystem() {
        return new FlyingItemSystem();
    }

    /**
     * @return the system counting a harvest by hand.
     */
    @Nonnull
    public EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> handPickupSystem() {
        return new HandPickupSystem();
    }

    private static long tick(@Nonnull Store<EntityStore> store) {
        return store.getExternalData().getWorld().getTick();
    }

    /**
     * Only the ground can hand back what the player threw, so a harvest by hand is told apart.
     */
    private static void count(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> player, @Nonnull ItemStack stack, boolean fromGround) {
        var playerRef = store.getComponent(player, PlayerRef.getComponentType());
        var questStoreComponent = store.getComponent(player, QuestStoreComponent.getComponentType());
        if (playerRef == null || questStoreComponent == null) return;

        ItemExchange exchange = fromGround ? ItemExchange.GROUND_PICKUP : ItemExchange.HARVEST;

        QuestProgressionService.get()
                               .progress(new ItemExchangeVisitor(playerRef.getUuid(), exchange, stack.getItemId(), stack.getQuantity()), questStoreComponent.getQuestIds());
    }

    private record TakenStack(@Nonnull ItemStack stack, long tick) {}

    /**
     * Sees every item leave the world, despawning and merging included; only the mark a player's
     * pickup leaves makes one worth noting. A pickup the inventory cannot hold whole leaves the item
     * on the ground, lighter, and is not seen here.
     */
    private final class LeftGroundSystem extends RefSystem<EntityStore> {

        @Override
        public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {}

        @Override
        public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            var item = store.getComponent(ref, ItemComponent.getComponentType());
            if (item == null || !item.isRemovedByPlayerPickup()) return;

            ItemStack stack = item.getItemStack();
            if (stack == null || stack.isEmpty()) return;

            taken.set(new TakenStack(stack, tick(store)));
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return ItemComponent.getComponentType();
        }
    }

    /**
     * The copy only carries the item's id, never how many were taken, which is why the removal
     * before it is needed at all.
     */
    private final class FlyingItemSystem extends RefSystem<EntityStore> {

        @Override
        public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
            TakenStack takenStack = taken.get();
            if (takenStack == null) return;

            taken.remove();
            if (takenStack.tick() != tick(store)) return;

            var pickup = store.getComponent(ref, PickupItemComponent.getComponentType());
            var copy = store.getComponent(ref, ItemComponent.getComponentType());
            if (pickup == null || copy == null || copy.getItemStack() == null) return;
            if (!takenStack.stack().getItemId().equals(copy.getItemStack().getItemId())) return;

            Ref<EntityStore> taker = pickup.getTargetRef();
            if (taker == null || !taker.isValid()) return;

            count(store, taker, takenStack.stack(), true);
        }

        @Override
        public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {}

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return Query.and(PickupItemComponent.getComponentType(), ItemComponent.getComponentType());
        }
    }

    /**
     * Last, so that any other system had its chance to cancel the harvest or change what it yields.
     */
    private static final class HandPickupSystem extends EntityEventSystem<EntityStore, InteractivelyPickupItemEvent> {

        private HandPickupSystem() {
            super(InteractivelyPickupItemEvent.class);
        }

        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractivelyPickupItemEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack == null || stack.isEmpty()) return;

            count(store, chunk.getReferenceTo(index), stack, false);
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return Query.and(PlayerRef.getComponentType(), QuestStoreComponent.getComponentType());
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return RootDependency.lastSet();
        }
    }
}
