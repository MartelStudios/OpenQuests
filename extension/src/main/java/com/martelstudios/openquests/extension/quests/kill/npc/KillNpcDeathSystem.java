package com.martelstudios.openquests.extension.quests.kill.npc;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.extension.quests.kill.KillDeathSystem;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Restricts {@link KillNpcQuestProgression} to NPC deaths.
 */
public class KillNpcDeathSystem extends KillDeathSystem {

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType());
    }

    @Override
    protected void onKill(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> victimRef, @Nonnull UUID killerId, @Nonnull QuestStoreComponent killerQuests) {
        var victim = store.getComponent(victimRef, NPCEntity.getComponentType());
        if (victim == null) return;

        QuestProgressionService.get()
                               .progress(new KillNpcQuestVisitor(killerId, victim), killerQuests.questsRecord.getAllIds());
    }
}
