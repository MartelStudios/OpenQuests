package com.martelstudios.opendialogues.commands.characters;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractTargetEntityCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.runtime.CharacterService;

import javax.annotation.Nonnull;
import java.util.List;

import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.isNpc;
import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.key;
import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.text;

/**
 * {@code /oc unassign}: takes the targeted NPC's character back; its draws stay, so it does not redraw.
 */
class UnassignCommand extends AbstractTargetEntityCommand {

    private final CharacterService characters;

    UnassignCommand(@Nonnull CharacterService characters) {
        super("unassign", key("unassign.desc"));
        this.characters = characters;
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull List<Ref<EntityStore>> targets, @Nonnull World world, @Nonnull Store<EntityStore> store) {
        for (Ref<EntityStore> ref : targets) {
            if (!isNpc(context, store, ref)) continue;
            String characterId = characters.unassign(store, ref);
            context.sendMessage(characterId == null ? text("unassign.nobody") : text("unassign.done").param("character", characterId));
        }
    }
}
