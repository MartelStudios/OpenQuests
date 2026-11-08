package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.instructions.Instruction;
import com.hypixel.hytale.server.npc.interactions.UseNPCInteraction;
import com.hypixel.hytale.server.npc.role.Role;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Which NPCs may carry a character: an NPC whose F another mod took is never ours, one whose role
 * answers F itself only when a distribution asks to override it. Mods veto more through the drawing event.
 */
public enum NpcEligibility {

    /** Not an NPC, or its F belongs to another mod. */
    None,
    /** Its role answers F itself, such as a merchant. */
    OwnInteraction,
    /** A plain NPC. */
    Plain;

    /**
     * @return how far the mod may give the NPC a character.
     */
    @Nonnull
    public static NpcEligibility of(@Nullable ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> ref) {
        if (accessor == null) return None;
        NPCEntity entity = accessor.getComponent(ref, NPCEntity.getComponentType());
        if (entity == null) return None;
        Interactions interactions = accessor.getComponent(ref, Interactions.getComponentType());
        if (interactions == null || !UseNPCInteraction.DEFAULT_ID.equals(interactions.getInteractionId(InteractionType.Use))) return None;
        Role role = entity.getRole();
        Instruction instruction = role == null ? null : role.getInteractionInstruction();
        return instruction != null && instruction.componentCount() > 1 ? OwnInteraction : Plain;
    }

    /**
     * @param overrideInteraction whether the character may replace the role's own F.
     * @return whether the NPC may receive a character.
     */
    public boolean allows(boolean overrideInteraction) {
        return this == Plain || this == OwnInteraction && overrideInteraction;
    }
}
