package com.martelstudios.opendialogues.nameplates;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.NameplateUpdate;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Sends each viewer the plate in their language, when they first see the entity or change language,
 * as the vanilla nameplate system does with one text for all.
 */
public class TranslatedNameplateSystem extends EntityTickingSystem<EntityStore> {

    @Nonnull
    private final ComponentType<EntityStore, EntityTrackerSystems.Visible> visibleType;
    @Nonnull
    private final ComponentType<EntityStore, TranslatedNameplate> plateType;
    @Nonnull
    private final Query<EntityStore> query;

    public TranslatedNameplateSystem(@Nonnull ComponentType<EntityStore, EntityTrackerSystems.Visible> visibleType, @Nonnull ComponentType<EntityStore, TranslatedNameplate> plateType) {
        this.visibleType = visibleType;
        this.plateType = plateType;
        this.query = Query.and(visibleType, plateType);
    }

    @Nullable
    @Override
    public SystemGroup<EntityStore> getGroup() {
        return EntityTrackerSystems.QUEUE_UPDATE_GROUP;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    /**
     * Not parallel: it reads the viewers' {@link PlayerRef}, other entities of the store.
     */
    @Override
    public boolean isParallel(int archetypeChunkSize, int taskCount) {
        return false;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        EntityTrackerSystems.Visible visible = chunk.getComponent(index, visibleType);
        TranslatedNameplate plate = chunk.getComponent(index, plateType);
        if (visible == null || plate == null) return;
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        I18nModule i18n = I18nModule.get();
        for (Map.Entry<Ref<EntityStore>, EntityTrackerSystems.EntityViewer> entry : visible.visibleTo.entrySet()) {
            PlayerRef player = store.getComponent(entry.getKey(), PlayerRef.getComponentType());
            if (player == null) continue;
            String language = player.getLanguage();
            if (!plate.needsSend(entry.getKey(), language, visible.newlyVisibleTo.containsKey(entry.getKey()))) continue;
            entry.getValue().queueUpdate(ref, new NameplateUpdate(NameplateTexts.text(i18n::getMessage, language, I18nModule.DEFAULT_LANGUAGE, plate.getKey())));
        }
        plate.retainViewers(visible.visibleTo.keySet());
    }
}
