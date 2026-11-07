package com.martelstudios.openquests.extension.quests.stat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsSystems;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Reads, once per tick, what the players running a lose stat quest lost, and hands it to those
 * quests. Runs after every stat change of the tick and before the server clears the change log.
 */
public class LoseStatTickingSystem extends EntityTickingSystem<EntityStore> {

    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
        new SystemDependency<>(Order.AFTER, EntityStatsSystems.EntityTrackerUpdate.class),
        new SystemDependency<>(Order.BEFORE, EntityStatsSystems.ClearChanges.class));

    private final QuestProgressionService progression;

    public LoseStatTickingSystem(@Nonnull QuestProgressionService progression) {
        this.progression = progression;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), EntityStatMap.getComponentType(), LoseStatQuestListener.getComponentType());
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var stats = archetypeChunk.getComponent(index, EntityStatMap.getComponentType());
        var listener = archetypeChunk.getComponent(index, LoseStatQuestListener.getComponentType());

        if (playerRef == null || stats == null || listener == null) return;

        progression.progress(new LoseStatQuestVisitor(playerRef.getUuid(), stat -> lossOf(stats, stat)), listener.getQuestIds());
    }

    private static double lossOf(@Nonnull EntityStatMap stats, @Nonnull String stat) {
        int statIndex = EntityStatType.getAssetMap().getIndex(stat);
        if (statIndex < 0) return 0;

        return StatLoss.measure(stats.getSelfUpdates().get(statIndex), stats.getSelfStatValues().get(statIndex));
    }
}
