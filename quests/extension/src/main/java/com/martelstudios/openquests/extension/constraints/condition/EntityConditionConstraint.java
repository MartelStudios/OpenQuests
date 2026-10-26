package com.martelstudios.openquests.extension.constraints.condition;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.condition.Condition;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.constraints.QuestConstraint;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Counts a player's progress only while their entity meets every condition listed: sprinting,
 * out of combat, under an effect. They are the game's own, the ones stat regeneration reads, so a
 * condition another mod registers works here too.
 */
public class EntityConditionConstraint extends QuestConstraint {

    public static final BuilderCodec<EntityConditionConstraint> CODEC = BuilderCodec.builder(EntityConditionConstraint.class, EntityConditionConstraint::new, QuestConstraint.BASE_CODEC)
                                                                                    .append(new KeyedCodec<>("Conditions", new ArrayCodec<>(Condition.CODEC, Condition[]::new), true), (constraint, conditions) -> constraint.conditions = conditions, constraint -> constraint.conditions)
                                                                                    .addValidator(Validators.nonEmptyArray())
                                                                                    .add()
                                                                                    .build();

    protected Condition[] conditions;

    protected EntityConditionConstraint() {}

    /**
     * Conditions read the entity, which only its world thread may do: asked from anywhere else, or
     * for a player between worlds, the action does not count.
     */
    @Override
    public boolean allowsProgress(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID actorId) {
        PlayerRef player = Universe.get().getPlayer(actorId);
        if (player == null) return false;

        Ref<EntityStore> reference = player.getReference();
        if (reference == null || !reference.isValid()) return false;

        Store<EntityStore> store = reference.getStore();
        if (!store.isInThread()) return false;

        // World time, which is what the conditions measuring a delay compare against
        return Condition.allConditionsMet(store, reference, store.getResource(TimeResource.getResourceType()).getNow(), conditions);
    }

    /**
     * @return the conditions the player's entity has to meet, all of them.
     */
    @Nonnull
    public Condition[] getConditions() {
        return conditions;
    }
}
