package com.martelstudios.opendialogues.commands.characters;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.martelstudios.opendialogues.characters.runtime.CharacterService;
import com.martelstudios.opendialogues.characters.runtime.CharacterSettings;

import javax.annotation.Nonnull;

/**
 * {@code /ocharacters}, alias {@code /oc}: lets admins inspect and steer which character NPCs carry.
 */
public class CharacterCommand extends AbstractCommandCollection {

    public CharacterCommand(@Nonnull CharacterService characters, @Nonnull Config<CharacterSettings> settings) {
        super("ocharacters", key("desc"));
        addAliases("oc");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN);
        addSubCommand(new InfoCommand(characters));
        addSubCommand(new AssignCommand(characters));
        addSubCommand(new UnassignCommand(characters));
        addSubCommand(new ResetCommand(characters));
        addSubCommand(new DrawsCommand(settings));
    }

    /**
     * @return the full translation key of a text of these commands.
     */
    @Nonnull
    static String key(@Nonnull String suffix) {
        return "opendialogues.commands.characters." + suffix;
    }

    /**
     * @return the translated text of these commands under that suffix.
     */
    @Nonnull
    static Message text(@Nonnull String suffix) {
        return Message.translation(key(suffix));
    }

    /**
     * Tells the sender when a targeted entity is no NPC, which carries no character.
     *
     * @return whether the entity is an NPC.
     */
    static boolean isNpc(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        if (ref.isValid() && store.getComponent(ref, NPCEntity.getComponentType()) != null) return true;
        context.sendMessage(text("notNpc"));
        return false;
    }
}
