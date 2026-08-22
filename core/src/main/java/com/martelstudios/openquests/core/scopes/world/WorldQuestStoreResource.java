package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestCorePlugin;
import com.martelstudios.openquests.core.stores.QuestsRecord;

import javax.annotation.Nullable;

/**
 * Per-world index of quest ids shared by every player of that world. Mirrors
 * {@link QuestStoreComponent} (per-player), but lives on the world's {@code EntityStore}
 * as a {@link Resource} instead of a {@link Component} since it isn't owned by a single entity.
 */
public class WorldQuestStoreResource implements Resource<EntityStore> {
    public static final BuilderCodec<WorldQuestStoreResource> CODEC = BuilderCodec.builder(WorldQuestStoreResource.class, WorldQuestStoreResource::new)
                                                                                  .append(new KeyedCodec<>("QuestStore", QuestsRecord.CODEC), (resource, quests) -> resource.questsRecord = quests, (resource) -> resource.questsRecord)
                                                                                  .add()
                                                                                  .build();

    public QuestsRecord questsRecord = new QuestsRecord();

    public WorldQuestStoreResource() {
    }

    public WorldQuestStoreResource(WorldQuestStoreResource other) {
        this.questsRecord = other.questsRecord.clone();
    }

    @Nullable
    @Override
    public Resource<EntityStore> clone() {
        return new WorldQuestStoreResource(this);
    }

    public static ResourceType<EntityStore, WorldQuestStoreResource> getResourceType() {
        return OpenQuestCorePlugin.get().getWorldStoreResourceType();
    }

    /**
     * Pulls this world's quests into the datastore. Mirrors {@link QuestStoreComponent#loadQuests()}.
     */
    public void loadQuests() {
        questsRecord.loadAll();
    }
}
