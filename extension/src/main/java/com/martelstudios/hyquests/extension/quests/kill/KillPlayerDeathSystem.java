package com.martelstudios.hyquests.extension.quests.kill;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.hyquests.core.services.QuestProgressionService;
import com.martelstudios.hyquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Watches players dying, so only a player kill can progress a {@link KillPlayerQuestProgression}.
 */
public class KillPlayerDeathSystem extends KillDeathSystem {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType());
    }

    @Override
    protected void onKill(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> victimRef, @Nonnull UUID killerId, @Nonnull QuestStoreComponent killerQuests) {
        var victim = store.getComponent(victimRef, PlayerRef.getComponentType());
        if (victim == null) return;

        QuestProgressionService.get()
                               .progress(new KillPlayerQuestVisitor(killerId, victim), killerQuests.questsRecord.getAllIds());
    }
}
