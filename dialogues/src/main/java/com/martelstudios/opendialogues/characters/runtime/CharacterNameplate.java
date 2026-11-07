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
import com.martelstudios.opendialogues.nameplates.NameplateTexts;
import com.martelstudios.opendialogues.nameplates.TranslatedNameplate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shows the NPC's display name above its head in each player's language, and as its display name.
 * A vanilla nameplate set by hand or by the role wins over ours.
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
        Step step = decide(context.identity().getNameplate(), wanted);
        if (step == Step.Remove) {
            remove(context);
            return;
        }
        if (wanted == null) return;
        boolean wrote = displayNames(context.accessor(), context.npc(), wanted, step == Step.Replace);
        plate(context.accessor(), context.npc(), wanted);
        // A drawn name already shown was written by this mod before it remembered its nameplate.
        if (step == Step.Replace || step == Step.FillMissing && (wrote || context.identity().getName() != null)) {
            context.identity().setNameplate(wanted);
        }
    }

    /**
     * Takes back the translated plate, always ours, and the display names only when this mod wrote them.
     */
    public static void remove(@Nonnull CharacterContext context) {
        if (context.accessor() == null) return;
        context.accessor().tryRemoveComponent(context.npc(), TranslatedNameplate.getComponentType());
        if (context.identity().getNameplate() == null) return;
        context.accessor().tryRemoveComponent(context.npc(), PersistentDisplayName.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), DisplayNameComponent.getComponentType());
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
     * @return whether at least one display name component now holds the name.
     */
    private static boolean displayNames(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> npc, @Nonnull String name, boolean replace) {
        boolean wrote = false;
        if (replace || accessor.getComponent(npc, PersistentDisplayName.getComponentType()) == null) {
            accessor.putComponent(npc, PersistentDisplayName.getComponentType(), new PersistentDisplayName(Message.translation(name)));
            wrote = true;
        }
        if (replace || accessor.getComponent(npc, DisplayNameComponent.getComponentType()) == null) {
            accessor.putComponent(npc, DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.translation(name)));
            wrote = true;
        }
        return wrote;
    }

    private static void plate(@Nonnull ComponentAccessor<EntityStore> accessor, @Nonnull Ref<EntityStore> npc, @Nonnull String name) {
        Nameplate vanilla = accessor.getComponent(npc, Nameplate.getComponentType());
        if (vanilla != null) {
            boolean ours = NameplateTexts.isOurs(vanilla.getText(), name, I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, name));
            // A plate set by hand or by the role wins; ours is added on the next scan, not in the removal's packet.
            if (ours) accessor.tryRemoveComponent(npc, Nameplate.getComponentType());
            else accessor.tryRemoveComponent(npc, TranslatedNameplate.getComponentType());
            return;
        }
        TranslatedNameplate translated = accessor.getComponent(npc, TranslatedNameplate.getComponentType());
        if (translated == null) accessor.putComponent(npc, TranslatedNameplate.getComponentType(), new TranslatedNameplate(name));
        else translated.setKey(name);
    }
}
