package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.modules.interaction.Interactions;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.instructions.Instruction;
import com.hypixel.hytale.server.npc.interactions.UseNPCInteraction;
import com.hypixel.hytale.server.npc.role.Role;

import javax.annotation.Nonnull;

/**
 * Which NPCs may carry a character: an NPC whose role answers F itself, or whose F another mod
 * took, is not ours to change. Mods veto more through the drawing event.
 */
public final class NpcEligibility {

    private NpcEligibility() {}

    /**
     * @return whether the NPC is a plain one the mod may give a character to.
     */
    public static boolean test(@Nonnull CharacterNpc npc) {
        if (npc.accessor() == null) return false;
        NPCEntity entity = npc.accessor().getComponent(npc.ref(), NPCEntity.getComponentType());
        if (entity == null) return false;
        Role role = entity.getRole();
        Instruction instruction = role == null ? null : role.getInteractionInstruction();
        if (instruction != null && instruction.componentCount() > 1) return false;
        Interactions interactions = npc.accessor().getComponent(npc.ref(), Interactions.getComponentType());
        return interactions != null && UseNPCInteraction.DEFAULT_ID.equals(interactions.getInteractionId(InteractionType.Use));
    }
}
