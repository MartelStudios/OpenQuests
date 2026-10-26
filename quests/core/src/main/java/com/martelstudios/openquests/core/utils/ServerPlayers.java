package com.martelstudios.openquests.core.utils;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The players of the running server, read off its universe.
 */
public final class ServerPlayers implements Players {

    @Override
    public boolean isOnline(@Nonnull UUID playerId) {
        return Universe.get().getPlayer(playerId) != null;
    }

    @Nonnull
    @Override
    public Collection<UUID> online() {
        return Universe.get().getPlayers().stream().map(PlayerRef::getUuid).toList();
    }

    @Override
    public void update(@Nonnull UUID playerId, @Nonnull Consumer<EntityComponents> action) {
        EntityComponents.update(playerId, action);
    }
}
