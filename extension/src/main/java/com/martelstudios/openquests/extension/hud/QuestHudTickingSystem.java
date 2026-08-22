package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pushes each player's in-progress quests to their {@link QuestTrackerHud} every tick (the HUD
 * itself throttles how often it actually sends an update to the client).
 * <p>
 * For now "shown on the HUD" just means "in progress" — there's no explicit tracked-quest
 * selection yet (see {@link QuestStoreComponent}); that can replace this filter later without
 * touching the HUD itself.
 */
public class QuestHudTickingSystem extends EntityTickingSystem<EntityStore> {
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(PlayerRef.getComponentType(), Player.getComponentType(), QuestStoreComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        var playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        var player = archetypeChunk.getComponent(index, Player.getComponentType());
        var questStoreComponent = archetypeChunk.getComponent(index, QuestStoreComponent.getComponentType());
        if (playerRef == null || player == null || questStoreComponent == null) return;

        // Gathering is pointless while the HUD would throttle the push away
        QuestTrackerHud hud = QuestTrackerHud.get(player, playerRef);
        if (!hud.shouldUpdate()) return;

        List<AbstractQuestProgression<?>> inProgress = new ArrayList<>();
        for (UUID questId : questStoreComponent.questsRecord.getAllIds()) {
            var quest = QuestProgressionService.get().getQuest(questId);
            if (quest != null && quest.getState() == QuestState.IN_PROGRESS) {
                inProgress.add(quest);
            }
        }

        hud.pushUpdate(inProgress);
    }
}
