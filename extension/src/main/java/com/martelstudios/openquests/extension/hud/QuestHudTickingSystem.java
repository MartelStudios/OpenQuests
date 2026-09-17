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
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Redraws a player's {@link QuestTrackerHud} on the tick after something of theirs changed. What
 * changed is not worked out here: {@link QuestHudRefresh} is told by the events that know, and a
 * player nothing happened to costs one lookup and nothing else.
 *
 * <p>Hands the panel every quest the player holds: which ones earn a line is the panel's to decide.
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
        if (playerRef == null || !QuestHudRefresh.consume(playerRef.getUuid())) return;

        var player = archetypeChunk.getComponent(index, Player.getComponentType());
        var questStoreComponent = archetypeChunk.getComponent(index, QuestStoreComponent.getComponentType());
        if (player == null || questStoreComponent == null) return;

        QuestTrackerHud hud = QuestTrackerHud.get(player, playerRef);

        // Gathering is pointless while the HUD would throttle the push away, and the mark is put
        // back so that what happened is drawn once the floor has passed rather than dropped
        if (!hud.shouldUpdate()) {
            QuestHudRefresh.mark(playerRef.getUuid());
            return;
        }

        List<AbstractQuestProgression<?>> quests = new ArrayList<>();
        for (UUID questId : questStoreComponent.getQuestIds()) {
            var quest = QuestProgressionService.get().getQuest(questId);
            if (quest != null) quests.add(quest);
        }

        hud.pushUpdate(quests);
    }
}
