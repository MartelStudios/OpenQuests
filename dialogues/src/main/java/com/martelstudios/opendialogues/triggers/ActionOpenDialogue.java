package com.martelstudios.opendialogues.triggers;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.corecomponents.ActionBase;
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport;
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider;

import javax.annotation.Nonnull;

/**
 * Opens a dialogue for the player an NPC is interacting with, when they press F on it.
 */
public class ActionOpenDialogue extends ActionBase {

    @Nonnull
    private final String dialogue;

    public ActionOpenDialogue(@Nonnull BuilderActionOpenDialogue builder, @Nonnull BuilderSupport support) {
        super(builder);
        this.dialogue = builder.getDialogue(support);
    }

    @Override
    public boolean canExecute(@Nonnull Ref<EntityStore> ref, @Nonnull ExecutionSupport support, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
        return super.canExecute(ref, support, sensorInfo, dt, store) && support.getStateSupport().getInteractionIterationTarget() != null;
    }

    @Override
    public boolean execute(@Nonnull Ref<EntityStore> ref, @Nonnull ExecutionSupport support, InfoProvider sensorInfo, double dt, @Nonnull Store<EntityStore> store) {
        super.execute(ref, support, sensorInfo, dt, store);
        Ref<EntityStore> target = support.getStateSupport().getInteractionIterationTarget();
        PlayerRef player = target == null ? null : store.getComponent(target, PlayerRef.getComponentType());
        if (player != null) DialogueTriggers.openFor(player, dialogue, "NPC role '" + support.getName() + "'");
        return true;
    }
}
