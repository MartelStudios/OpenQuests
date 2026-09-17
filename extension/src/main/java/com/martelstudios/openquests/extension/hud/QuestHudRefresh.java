package com.martelstudios.openquests.extension.hud;

import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.martelstudios.openquests.core.events.QuestStateChangedEvent;
import com.martelstudios.openquests.core.events.QuestUpdatedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.player.events.QuestAddedToPlayerStoreEvent;
import com.martelstudios.openquests.core.scopes.player.events.QuestRemovedFromPlayerStoreEvent;
import com.martelstudios.openquests.extension.track.events.QuestTrackedEvent;
import com.martelstudios.openquests.extension.track.events.QuestUntrackedEvent;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which players have something new to see on their panel. The events that change a quest land on
 * any thread, where drawing has to happen on the player's own; so they leave a mark here, and the
 * ticking system picks it up on its next pass.
 *
 * <p>A signal rather than an index: each mark is taken away by the pass that acts on it, and a
 * player who never changes anything is never named.
 */
public final class QuestHudRefresh {

    private static final Set<UUID> MARKED = ConcurrentHashMap.newKeySet();

    private QuestHudRefresh() {}

    /**
     * Asks for this player's panel to be redrawn on the next tick.
     */
    public static void mark(@Nonnull UUID playerId) {
        MARKED.add(playerId);
    }

    /**
     * @return {@code true} if this player had something waiting, which the caller now owes them.
     */
    public static boolean consume(@Nonnull UUID playerId) {
        return MARKED.remove(playerId);
    }

    /**
     * One draw per session, so a player arrives to a panel rather than to nothing.
     */
    public static void handlePlayerConnect(@Nonnull PlayerConnectEvent event) {
        mark(event.getPlayerRef().getUuid());
    }

    /**
     * Only fired for a quest that actually moved, so a counter creeping up a metre at a time marks
     * a few times a second rather than every tick.
     */
    public static void handleQuestUpdated(@Nonnull QuestUpdatedEvent event) {
        markHolders(event.getQuest());
    }

    public static void handleQuestStateChanged(@Nonnull QuestStateChangedEvent event) {
        markHolders(event.getQuest());
    }

    /**
     * Following a quest changes nothing about the quest, so nothing else would say it: the panel
     * would sit on a stale line until the quest next moved.
     */
    public static void handleQuestTracked(@Nonnull QuestTrackedEvent event) {
        markHolders(event.getQuest());
    }

    public static void handleQuestUntracked(@Nonnull QuestUntrackedEvent event) {
        markHolders(event.getQuest());
    }

    public static void handleQuestAddedToPlayerStore(@Nonnull QuestAddedToPlayerStoreEvent event) {
        mark(event.getPlayerId());
    }

    public static void handleQuestRemovedFromPlayerStore(@Nonnull QuestRemovedFromPlayerStoreEvent event) {
        mark(event.getPlayerId());
    }

    /**
     * Those who gave the quest up too: a line leaving the panel is as much a change as one arriving.
     */
    private static void markHolders(@Nonnull AbstractQuestProgression<?> quest) {
        quest.getPlayers().forEach(QuestHudRefresh::mark);
        quest.getAbandonedPlayers().forEach(QuestHudRefresh::mark);
    }
}
