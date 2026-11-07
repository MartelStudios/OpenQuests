package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDisplayName;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shows the NPC's display name above its head. The nameplate is plain text, so it shows in the
 * server's default language; dialogues show the name in each player's language.
 */
public final class CharacterNameplate {

    /**
     * What to do with the name components, from the name this mod last wrote and the one it wants now.
     */
    enum Step { Keep, FillMissing, Replace, Remove }

    private CharacterNameplate() {}

    /**
     * Brings the components in line with the display name, after the traits and on every reload.
     */
    public static void apply(@Nonnull CharacterContext context) {
        if (context.accessor() == null) return;
        String wanted = context.character().displayName(context.identity());
        switch (decide(context.identity().getNameplate(), wanted)) {
            case Keep -> {}
            case FillMissing -> {
                // A drawn name already shown was written by this mod before it remembered its nameplate.
                boolean wrote = fill(context.accessor(), context.npc(), wanted, false);
                if (wrote || context.identity().getName() != null) context.identity().setNameplate(wanted);
            }
            case Replace -> {
                fill(context.accessor(), context.npc(), wanted, true);
                context.identity().setNameplate(wanted);
            }
            case Remove -> remove(context);
        }
    }

    /**
     * Takes back only a name this mod wrote, so one the role or another mod set stays.
     */
    public static void remove(@Nonnull CharacterContext context) {
        if (context.accessor() == null || context.identity().getNameplate() == null) return;
        context.accessor().tryRemoveComponent(context.npc(), PersistentDisplayName.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), DisplayNameComponent.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), Nameplate.getComponentType());
        context.identity().setNameplate(null);
    }

    /**
     * A first name fills only what the NPC lacks, so a name its role set stays; a name this mod
     * wrote follows every change of the character's.
     */
    @Nonnull
    static Step decide(@Nullable String written, @Nullable String wanted) {
        if (wanted == null) return written == null ? Step.Keep : Step.Remove;
        if (written == null) return Step.FillMissing;
        return written.equals(wanted) ? Step.Keep : Step.Replace;
    }

    /**
     * @return whether at least one component now holds the name.
     */
    private static boolean fill(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> npc, @Nonnull String name, boolean replace) {
        boolean wrote = false;
        if (replace || accessor.getComponent(npc, PersistentDisplayName.getComponentType()) == null) {
            accessor.putComponent(npc, PersistentDisplayName.getComponentType(), new PersistentDisplayName(Message.translation(name)));
            wrote = true;
        }
        if (replace || accessor.getComponent(npc, DisplayNameComponent.getComponentType()) == null) {
            accessor.putComponent(npc, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.translation(name)));
            wrote = true;
        }
        if (replace || accessor.getComponent(npc, Nameplate.getComponentType()) == null) {
            String text = I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, name);
            accessor.putComponent(npc, Nameplate.getComponentType(), new Nameplate(text != null ? text : name));
            wrote = true;
        }
        return wrote;
    }
}
