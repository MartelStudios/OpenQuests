package com.martelstudios.hyquests.services;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.hyquests.HyQuestsPlugin;
import com.martelstudios.hyquests.stores.WorldQuestStoreResource;

import javax.annotation.Nonnull;
import java.util.UUID;

public class WorldQuestService {
    private static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static WorldQuestService get() {
        return HyQuestsPlugin.get().worldQuestService;
    }

    public void addQuestToWorldStore(@Nonnull UUID questId, @Nonnull World world) {
        var store = world.getEntityStore().getStore();
        store.getResource(WorldQuestStoreResource.getResourceType()).questsRecord.register(questId);
        PlayerQuestService.get().addQuestToPlayerStore(questId, world.getPlayerRefs());
    }

    public void removeQuestFromWorldStore(@Nonnull UUID questId, @Nonnull World world) {
        var store = world.getEntityStore().getStore();
        store.getResource(WorldQuestStoreResource.getResourceType()).questsRecord.unregister(questId);
        PlayerQuestService.get().removeQuestFromPlayerStore(questId, world.getPlayerRefs());
    }
}
