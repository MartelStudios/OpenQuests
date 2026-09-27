package com.martelstudios.openquests.extension.constraints.death;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.extension.constraints.ConstraintEndVisitor;

import javax.annotation.Nonnull;

/**
 * Fails the quests of a player who dies, among those whose asset asks for it. Only the victim's own
 * quests are walked, so a death costs what that one player holds.
 */
public class FailOnDeathSystem extends DeathSystems.OnDeathSystem {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), QuestStoreComponent.getComponentType());
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> victimRef, @Nonnull DeathComponent deathComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = store.getComponent(victimRef, PlayerRef.getComponentType());
        var questStore = store.getComponent(victimRef, QuestStoreComponent.getComponentType());
        if (playerRef == null || questStore == null) return;

        QuestProgressionService.get()
                               .progress(new ConstraintEndVisitor(playerRef.getUuid(), constraint -> ConstraintEndVisitor.failIf(constraint instanceof FailOnDeathConstraint)), questStore.getQuestIds());
    }
}
