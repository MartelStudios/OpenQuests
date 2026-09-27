package com.martelstudios.openquests.extension.quests.placeblock;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.extension.quests.block.BlockAction;
import com.martelstudios.openquests.extension.quests.block.BlockActionVisitor;
import com.martelstudios.openquests.extension.quests.block.PlacedBlocks;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Watches blocks being placed and hands each one to the quests that asked for it.
 */
public class PlaceBlockEventSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
    public PlaceBlockEventSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull PlaceBlockEvent event) {
        // The event is fired before the block moves and can still be called off. Whoever cancels it
        // after this runs is not heard, so a quest counts what was allowed rather than what landed.
        if (event.isCancelled()) return;

        var ref = chunk.getReferenceTo(index);

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var questStoreComponent = store.getComponent(ref, QuestStoreComponent.getComponentType());
        if (playerRef == null || questStoreComponent == null) return;

        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand == null) return;

        Vector3i target = event.getTargetBlock();
        String position = PlacedBlocks.position(store.getExternalData().getWorld().getName(), target.x(), target.y(), target.z());

        QuestProgressionService.get()
                               .progress(new BlockActionVisitor(playerRef.getUuid(), BlockAction.PLACE, itemInHand.getItemId(), position), questStoreComponent.getQuestIds());
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), QuestStoreComponent.getComponentType());
    }
}
