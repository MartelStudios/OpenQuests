package com.martelstudios.opendialogues.characters.runtime;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDisplayName;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.martelstudios.opendialogues.characters.traits.CharacterContext;

import javax.annotation.Nonnull;

/**
 * Shows the NPC's display name above its head. The nameplate is plain text, so it shows in the
 * server's default language; dialogues show the name in each player's language.
 */
public final class CharacterNameplate {

    private CharacterNameplate() {}

    /**
     * Fills only what the NPC lacks, so a name its role or another mod set stays.
     */
    public static void apply(@Nonnull CharacterContext context) {
        String name = context.character().displayName(context.identity());
        if (context.accessor() == null || name == null) return;
        if (context.accessor().getComponent(context.npc(), PersistentDisplayName.getComponentType()) == null) {
            context.accessor().putComponent(context.npc(), PersistentDisplayName.getComponentType(), new PersistentDisplayName(Message.translation(name)));
        }
        if (context.accessor().getComponent(context.npc(), DisplayNameComponent.getComponentType()) == null) {
            context.accessor().putComponent(context.npc(), DisplayNameComponent.getComponentType(), new DisplayNameComponent(Message.translation(name)));
        }
        if (context.accessor().getComponent(context.npc(), Nameplate.getComponentType()) == null) {
            String text = I18nModule.get().getMessage(I18nModule.DEFAULT_LANGUAGE, name);
            context.accessor().putComponent(context.npc(), Nameplate.getComponentType(), new Nameplate(text != null ? text : name));
        }
    }

    /**
     * Only for a revoked NPC, which lives on; a dead or removed one takes its components along.
     */
    public static void remove(@Nonnull CharacterContext context) {
        if (context.accessor() == null || context.character().displayName(context.identity()) == null) return;
        context.accessor().tryRemoveComponent(context.npc(), PersistentDisplayName.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), DisplayNameComponent.getComponentType());
        context.accessor().tryRemoveComponent(context.npc(), Nameplate.getComponentType());
    }
}
