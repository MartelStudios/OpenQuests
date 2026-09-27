package com.martelstudios.openquests.extension.constraints.location;

import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.extension.constraints.ConstraintEndVisitor;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fails the quests a player walks out of, for the {@code InWorld} constraints asking for it.
 *
 * <p>Entering a world is the one event saying where a player goes, so each one's last world is
 * remembered for the session. Leaving a world is also announced as a player logs out, and logging
 * out is not walking out: a quest survives a disconnection inside its world.
 */
public final class InWorldLeaveTracker {

    private final Map<UUID, String> lastWorlds = new ConcurrentHashMap<>();

    /**
     * Only a move from a matching world to one that is not counts, so a quest handed out before
     * the player ever reached its world is not failed on the way there.
     */
    public void handleAddPlayerToWorld(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        var holder = addPlayerToWorldEvent.getHolder();
        var playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        String entered = addPlayerToWorldEvent.getWorld().getName();
        String left = lastWorlds.put(playerRef.getUuid(), entered);
        if (left == null || left.equals(entered)) return;

        var questStore = holder.getComponent(QuestStoreComponent.getComponentType());
        if (questStore == null) return;

        QuestProgressionService.get()
                               .progress(new ConstraintEndVisitor(playerRef.getUuid(), constraint -> ConstraintEndVisitor.failIf(
                                   constraint instanceof InWorldConstraint inWorld && inWorld.failsOnLeave() && inWorld.matches(left) && !inWorld.matches(entered))), questStore.getQuestIds());
    }

    /**
     * Forgets the player, so the world they come back to is where their next session starts from.
     */
    public void handlePlayerDisconnect(@Nonnull PlayerDisconnectEvent playerDisconnectEvent) {
        lastWorlds.remove(playerDisconnectEvent.getPlayerRef().getUuid());
    }
}
