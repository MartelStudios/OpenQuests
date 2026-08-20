package com.martelstudios.hyquests.extension.quests.kill;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Progresses the killer's quests when something dies.
 */
public abstract class KillDeathSystem extends DeathSystems.OnDeathSystem {

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> victimRef, @Nonnull DeathComponent deathComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Damage deathInfo = deathComponent.getDeathInfo();
        if (deathInfo == null) return;
        if (!(deathInfo.getSource() instanceof Damage.EntitySource entitySource)) return;

        Ref<EntityStore> killerRef = entitySource.getRef();
        if (!killerRef.isValid()) return;

        var killerPlayerRef = store.getComponent(killerRef, PlayerRef.getComponentType());
        var killerQuests = store.getComponent(killerRef, QuestStoreComponent.getComponentType());
        if (killerPlayerRef == null || killerQuests == null) return;

        onKill(store, victimRef, killerPlayerRef.getUuid(), killerQuests);
    }

    protected abstract void onKill(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> victimRef, @Nonnull UUID killerId, @Nonnull QuestStoreComponent killerQuests);
}
