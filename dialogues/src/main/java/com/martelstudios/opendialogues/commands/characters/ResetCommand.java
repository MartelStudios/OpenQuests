package com.martelstudios.opendialogues.commands.characters;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opendialogues.characters.runtime.CharacterService;

import javax.annotation.Nonnull;
import java.util.List;

import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.key;
import static com.martelstudios.opendialogues.commands.characters.CharacterCommand.text;

/**
 * {@code /oc reset [dead|world]}: forgets every character of the universe, brings back the lost
 * ones, or forgets those of one world. NPCs out of loaded chunks settle when they are next met.
 */
class ResetCommand extends CommandBase {

    private final CharacterService characters;

    ResetCommand(@Nonnull CharacterService characters) {
        super("reset", key("reset.desc"));
        this.characters = characters;
        addSubCommand(new DeadCommand(characters));
        addSubCommand(new WorldCommand(characters));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        characters.resetUniverse();
        context.sendMessage(text("reset.universe"));
        settleLoadedWorlds(context, characters);
    }

    /**
     * Settles every loaded world on its own thread, each reporting how many NPCs it reset now.
     */
    private static void settleLoadedWorlds(@Nonnull CommandContext context, @Nonnull CharacterService characters) {
        for (World world : Universe.get().getWorlds().values()) {
            Store<EntityStore> store = world.getEntityStore().getStore();
            world.execute(() -> context.sendMessage(text("reset.world").param("world", world.getName()).param("count", characters.settleLoaded(store))));
        }
    }

    private static class DeadCommand extends CommandBase {

        private final CharacterService characters;

        DeadCommand(@Nonnull CharacterService characters) {
            super("dead", key("reset.dead.desc"));
            this.characters = characters;
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            List<String> freed = characters.resetDead();
            context.sendMessage(freed.isEmpty() ? text("reset.deadNone") : text("reset.dead").param("characters", String.join(", ", freed)));
            for (World world : Universe.get().getWorlds().values()) {
                Store<EntityStore> store = world.getEntityStore().getStore();
                world.execute(() -> characters.settleLoaded(store));
            }
        }
    }

    private static class WorldCommand extends AbstractWorldCommand {

        private final CharacterService characters;

        WorldCommand(@Nonnull CharacterService characters) {
            super("world", key("reset.world.desc"));
            this.characters = characters;
        }

        @Override
        protected void execute(@Nonnull CommandContext context, @Nonnull World world, @Nonnull Store<EntityStore> store) {
            context.sendMessage(text("reset.world").param("world", world.getName()).param("count", characters.resetWorld(store)));
        }
    }
}
