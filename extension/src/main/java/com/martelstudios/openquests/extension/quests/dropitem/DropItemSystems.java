package com.martelstudios.openquests.extension.quests.dropitem;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.extension.quests.item.ItemExchange;
import com.martelstudios.openquests.extension.quests.item.ItemExchangeVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts what a player throws out of their inventory on purpose. A throw arrives as a request naming
 * a slot, then as a drop naming the stack; a full inventory spilling at the player's feet only
 * drops, so a drop counts only when it answers a request made on the same tick.
 */
public final class DropItemSystems {

    /**
     * The tick each player last asked to throw something on. Written and read on the world thread
     * the player is in, but players of several worlds share it.
     */
    private final Map<UUID, Long> requests = new ConcurrentHashMap<>();

    /**
     * @return the system noting a player's request to throw.
     */
    @Nonnull
    public EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> requestSystem() {
        return new RequestSystem();
    }

    /**
     * @return the system counting the throw that answers it.
     */
    @Nonnull
    public EntityEventSystem<EntityStore, DropItemEvent.Drop> dropSystem() {
        return new DropSystem();
    }

    private static long tick(@Nonnull Store<EntityStore> store) {
        return store.getExternalData().getWorld().getTick();
    }

    private static Query<EntityStore> playersWithQuests() {
        return Query.and(PlayerRef.getComponentType(), QuestStoreComponent.getComponentType());
    }

    /**
     * Last, so that any other system had its chance to cancel the request first: a cancelled one
     * never reaches this one.
     */
    private final class RequestSystem extends EntityEventSystem<EntityStore, DropItemEvent.PlayerRequest> {

        private RequestSystem() {
            super(DropItemEvent.PlayerRequest.class);
        }

        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DropItemEvent.PlayerRequest event) {
            var playerRef = store.getComponent(chunk.getReferenceTo(index), PlayerRef.getComponentType());
            if (playerRef == null) return;

            requests.put(playerRef.getUuid(), tick(store));
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return playersWithQuests();
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return RootDependency.lastSet();
        }
    }

    /**
     * Last as well, for the same reason: another system may still cancel the throw or change the
     * stack it sends out.
     */
    private final class DropSystem extends EntityEventSystem<EntityStore, DropItemEvent.Drop> {

        private DropSystem() {
            super(DropItemEvent.Drop.class);
        }

        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DropItemEvent.Drop event) {
            var ref = chunk.getReferenceTo(index);

            var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            var questStoreComponent = store.getComponent(ref, QuestStoreComponent.getComponentType());
            if (playerRef == null || questStoreComponent == null) return;

            Long requestedOn = requests.remove(playerRef.getUuid());
            if (requestedOn == null || requestedOn != tick(store)) return;

            ItemStack dropped = event.getItemStack();
            if (dropped == null || dropped.isEmpty()) return;

            QuestProgressionService.get()
                                   .progress(new ItemExchangeVisitor(playerRef.getUuid(), ItemExchange.THROW, dropped.getItemId(), dropped.getQuantity()), questStoreComponent.getQuestIds());
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return playersWithQuests();
        }

        @Nonnull
        @Override
        public Set<Dependency<EntityStore>> getDependencies() {
            return RootDependency.lastSet();
        }
    }
}
