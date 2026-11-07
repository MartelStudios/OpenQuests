package com.martelstudios.opendialogues.nameplates;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.protocol.ComponentUpdateType;
import com.hypixel.hytale.protocol.NameplateUpdate;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Clears the plate on every viewer's screen when the component goes, as the vanilla nameplate does,
 * unless a vanilla plate is left to show.
 */
public class TranslatedNameplateRemoval extends RefChangeSystem<EntityStore, TranslatedNameplate> {

    @Nonnull
    private final ComponentType<EntityStore, EntityTrackerSystems.Visible> visibleType;
    @Nonnull
    private final ComponentType<EntityStore, TranslatedNameplate> plateType;
    @Nonnull
    private final Query<EntityStore> query;

    public TranslatedNameplateRemoval(@Nonnull ComponentType<EntityStore, EntityTrackerSystems.Visible> visibleType, @Nonnull ComponentType<EntityStore, TranslatedNameplate> plateType) {
        this.visibleType = visibleType;
        this.plateType = plateType;
        this.query = Query.and(visibleType, plateType);
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, TranslatedNameplate> componentType() {
        return plateType;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref, @Nonnull TranslatedNameplate component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref, TranslatedNameplate oldComponent, @Nonnull TranslatedNameplate newComponent, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref, @Nonnull TranslatedNameplate component, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        EntityTrackerSystems.Visible visible = store.getComponent(ref, visibleType);
        if (visible == null) return;
        // A vanilla plate left on the entity must stay on screen: resend its text rather than clear.
        Nameplate vanilla = store.getComponent(ref, Nameplate.getComponentType());
        for (EntityTrackerSystems.EntityViewer viewer : visible.visibleTo.values()) {
            if (vanilla != null) viewer.queueUpdate(ref, new NameplateUpdate(vanilla.getText()));
            else viewer.queueRemove(ref, ComponentUpdateType.Nameplate);
        }
    }
}
