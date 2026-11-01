package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.InteractableUpdate;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.system.EntityInteractableSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Shows the F prompt over NPCs carrying a character. The client reads it from the Interactable
 * update, which the engine sends without a hint, so this one follows it with the hint.
 */
public class CharacterHintSystem extends EntityTickingSystem<EntityStore> {

    private final CharacterService service;
    private final ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType;
    private final Set<Ref<EntityStore>> hinted = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private final ComponentType<EntityStore, EntityTrackerSystems.Visible> visibleType;
    private final Query<EntityStore> query;
    private final Set<Dependency<EntityStore>> dependencies = Set.of(new SystemDependency<>(Order.AFTER, EntityInteractableSystems.EntityTrackerUpdate.class));

    public CharacterHintSystem(@Nonnull CharacterService service, @Nonnull ComponentType<EntityStore, OpenDialoguesNPCIdentity> identityType) {
        this.service = service;
        this.identityType = identityType;
        this.visibleType = EntityModule.get().getVisibleComponentType();
        this.query = Query.and(identityType, visibleType, Interactable.getComponentType());
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return EntityTrackerSystems.QUEUE_UPDATE_GROUP;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    /**
     * Every viewer gets the hint once per load, since the engine resends its empty update to all of
     * them when the NPC turns interactable; later, only the viewers that just started seeing it.
     */
    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        OpenDialoguesNPCIdentity identity = archetypeChunk.getComponent(index, identityType);
        EntityTrackerSystems.Visible visible = archetypeChunk.getComponent(index, visibleType);
        if (identity == null || visible == null || identity.getCharacter() == null) return;
        String hint = service.hint(identity);
        if (hint == null) return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Map<Ref<EntityStore>, EntityTrackerSystems.EntityViewer> viewers = hinted.contains(ref) ? visible.newlyVisibleTo : visible.visibleTo;
        if (viewers.isEmpty()) return;
        InteractableUpdate update = new InteractableUpdate(hint);
        for (EntityTrackerSystems.EntityViewer viewer : viewers.values()) viewer.queueUpdate(ref, update);
        hinted.add(ref);
    }
}
