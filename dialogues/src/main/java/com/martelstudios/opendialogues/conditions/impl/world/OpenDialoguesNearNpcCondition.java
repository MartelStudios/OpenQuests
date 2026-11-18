package com.martelstudios.opendialogues.conditions.impl.world;

import com.hypixel.hytale.builtin.tagset.config.NPCGroup;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.support.WorldSupport;
import com.martelstudios.opendialogues.conditions.ConditionContext;
import com.martelstudios.opendialogues.conditions.CountBounds;
import com.martelstudios.opendialogues.conditions.DialogueCondition;
import com.martelstudios.opendialogues.dialogues.OpenDialogueAsset;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Holds while between {@code Min} and {@code Max} living NPCs of a role or a group stand within
 * {@code Radius} of the player: offer a hunt only where the prey is.
 */
public class OpenDialoguesNearNpcCondition extends DialogueCondition {

    public static final String TYPE = "NearNpc";

    public static final BuilderCodec<OpenDialoguesNearNpcCondition> CODEC = BuilderCodec.builder(OpenDialoguesNearNpcCondition.class, OpenDialoguesNearNpcCondition::new, DialogueCondition.BASE_CODEC)
                                                                                       .append(new KeyedCodec<>("Role", Codec.STRING), (condition, role) -> condition.role = role, condition -> condition.role)
                                                                                       .add()
                                                                                       .append(new KeyedCodec<>("Group", Codec.STRING), (condition, group) -> condition.group = group, condition -> condition.group)
                                                                                       .add()
                                                                                       .append(new KeyedCodec<>("Radius", Codec.DOUBLE, true), (condition, radius) -> condition.radius = radius, condition -> condition.radius)
                                                                                       .add()
                                                                                       .append(new KeyedCodec<>("Min", Codec.INTEGER), (condition, min) -> condition.min = min, condition -> condition.min)
                                                                                       .add()
                                                                                       .append(new KeyedCodec<>("Max", Codec.INTEGER), (condition, max) -> condition.max = max, condition -> condition.max)
                                                                                       .add()
                                                                                       .validator((condition, results) -> {
                                                                                           if ((condition.role == null) == (condition.group == null)) results.fail("Exactly one of Role or Group must be set");
                                                                                           String radius = NearbyCount.validateRadius(condition.radius);
                                                                                           if (radius != null) results.fail(radius);
                                                                                       })
                                                                                       .build();

    @Nullable
    protected String role;
    @Nullable
    protected String group;
    protected double radius;
    protected int min = 1;
    @Nullable
    protected Integer max;

    protected OpenDialoguesNearNpcCondition() {}

    /**
     * Fails off the player's world thread, where entities cannot be read.
     */
    @Override
    protected boolean evaluate(@Nonnull ConditionContext context) {
        if (!context.canReadEntity()) return false;
        Store<EntityStore> store = context.store();
        TransformComponent transform = store.getComponent(context.entity(), TransformComponent.getComponentType());
        if (transform == null) return false;

        Vector3d centre = transform.getPosition();
        List<Ref<EntityStore>> nearby = SpatialResource.getThreadLocalReferenceList();
        store.getResource(NPCPlugin.get().getNpcSpatialResource()).getSpatialStructure().collect(centre, radius, nearby);
        int groupIndex = group == null ? -1 : NPCGroup.getAssetMap().getIndexOrDefault(group, -1);
        double squared = radius * radius;
        return NearbyCount.holds(List.copyOf(nearby), ref -> counts(store, ref, centre, squared, groupIndex), min, max);
    }

    private boolean counts(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull Vector3d centre, double squared, int groupIndex) {
        if (!ref.isValid() || store.getComponent(ref, DeathComponent.getComponentType()) != null) return false;
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (npc == null || transform == null || transform.getPosition().distanceSquared(centre) > squared) return false;
        return role != null ? role.equals(npc.getRoleName()) : groupIndex >= 0 && WorldSupport.hasTagInGroup(groupIndex, npc.getRoleIndex());
    }

    @Nullable
    @Override
    public String validate(@Nonnull Function<String, OpenDialogueAsset> dialogues) {
        return CountBounds.validate(min, max);
    }
}
