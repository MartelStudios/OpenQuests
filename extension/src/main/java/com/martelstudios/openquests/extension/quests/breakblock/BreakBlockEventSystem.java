package com.martelstudios.openquests.extension.quests.breakblock;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.extension.quests.block.BlockAction;
import com.martelstudios.openquests.extension.quests.block.BlockActionVisitor;
import com.martelstudios.openquests.extension.quests.block.PlacedBlockMarks;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Watches blocks being broken, forgets the ones players had placed and hands each block to the quests
 * that asked for it.
 */
public class BreakBlockEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    public BreakBlockEventSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {
        // The event is fired before the block moves. Running last, this only sees what every other
        // system let through, so the world and the quests keep what actually landed
        if (event.isCancelled()) return;

        var ref = chunk.getReferenceTo(index);

        World world = store.getExternalData().getWorld();
        Vector3i target = event.getTargetBlock();
        boolean placedByPlayer = PlacedBlockMarks.consume(world, target.x(), target.y(), target.z());

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var questStoreComponent = store.getComponent(ref, QuestStoreComponent.getComponentType());
        if (playerRef == null || questStoreComponent == null) return;

        QuestProgressionService.get()
                               .progress(new BlockActionVisitor(playerRef.getUuid(), BlockAction.BREAK, event.getBlockType().getId(), placedByPlayer), questStoreComponent.getQuestIds());
    }

    /**
     * Every player, holding quests or not: the world remembers their blocks for whichever quest comes.
     */
    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return RootDependency.lastSet();
    }
}
