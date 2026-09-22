package com.martelstudios.openquests.extension.journal.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.opennavigation.services.NavigationService;
import com.martelstudios.openquests.extension.journal.navigation.JournalRoutes;

import javax.annotation.Nonnull;

/**
 * Opens the quest journal. Extends {@link AbstractPlayerCommand} because opening a page touches
 * the entity store, which only answers on its world thread.
 * <p>
 * Roots a history rather than opening a page: what the player was looking at before is not somewhere
 * to come back to.
 */
public class JournalCommand extends AbstractPlayerCommand {

    public JournalCommand() {
        super("ojournal", "Opens your quest journal");
        this.addAliases("journal", "quests");
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADVENTURER);
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        NavigationService.get().root(playerRef, JournalRoutes.journal(JournalRoutes.TAB_ACTIVE));
    }
}
