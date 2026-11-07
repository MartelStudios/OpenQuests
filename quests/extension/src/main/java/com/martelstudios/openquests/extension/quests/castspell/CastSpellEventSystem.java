package com.martelstudios.openquests.extension.quests.castspell;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.item.config.CoreItemAbility;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.interaction.event.InteractionChainStartEvent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.util.AbilityCastUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Counts a spell as its cast chain starts from an ability slot. The server has already refused a
 * spell on cooldown by then; the mana check the chain makes first is made here the same way, so a
 * cast that fizzles for want of mana does not count.
 */
public class CastSpellEventSystem extends EntityEventSystem<EntityStore, InteractionChainStartEvent> {

    private final QuestProgressionService progression;

    public CastSpellEventSystem(@Nonnull QuestProgressionService progression) {
        super(InteractionChainStartEvent.class);
        this.progression = progression;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionChainStartEvent event) {
        if (event.isCancelled()) return;

        InteractionContext context = event.getContext();
        CoreItemAbility spell = AbilityCastUtil.castAbility(context);
        if (spell == null || context.getOriginalItemType() == null) return;

        var ref = chunk.getReferenceTo(index);
        if (!canAfford(context, store.getComponent(ref, EntityStatMap.getComponentType()))) return;

        var playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        var questStoreComponent = store.getComponent(ref, QuestStoreComponent.getComponentType());
        if (playerRef == null || questStoreComponent == null) return;

        String spellId = context.getOriginalItemType().getId();
        progression.progress(new CastSpellQuestVisitor(playerRef.getUuid(), spellId, spell.getAbilityKeywords()), questStoreComponent.getQuestIds());
    }

    /**
     * Mirrors the {@code StatsCondition} with {@code UseAbilityCost} that opens every cast chain: a
     * spell with no cost, or cast in creative, always passes.
     */
    private static boolean canAfford(@Nonnull InteractionContext context, @Nullable EntityStatMap stats) {
        Int2FloatMap costs = AbilityCastUtil.resolveCostMap(context, false);
        if (costs == null) return true;
        if (stats == null) return false;

        for (Int2FloatMap.Entry cost : costs.int2FloatEntrySet()) {
            EntityStatValue stat = stats.get(cost.getIntKey());
            if (stat == null || stat.get() < cost.getFloatValue()) return false;
        }

        return true;
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), QuestStoreComponent.getComponentType());
    }

    /**
     * Last, so the support runes have scaled the cost and any other system has had its say on
     * cancelling the cast.
     */
    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return RootDependency.lastSet();
    }
}
