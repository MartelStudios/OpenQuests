package com.martelstudios.openquests.core.services;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStore;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStoreComponent;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A player's session, from the storage's point of view: everything they hold is read back when
 * they connect and written out when they leave.
 *
 * <p>This is what makes one database serve several servers: nothing of a player's quests lives in
 * their entity file, so the server they land on next reads the same rows the last one wrote.
 */
public class QuestPlayerStateService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final QuestProgressionStore progressionStore;
    private final QuestStorage storage;

    /**
     * The components of everyone online, held by reference: a {@code Store} only hands one over on
     * its own world thread, and neither the save pass nor a shutdown runs there. Safe to read from
     * anywhere, the ECS moving components by reference and their contents being concurrent.
     */
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public QuestPlayerStateService(@Nonnull JavaPlugin plugin, @Nonnull QuestProgressionStore progressionStore) {
        this.progressionStore = progressionStore;
        this.storage = progressionStore.getStorage();

        // EventPriority.FIRST, so the quests are back in the store before any scope resolves an id
        // against it and takes a miss for a quest that does exist
        plugin.getEventRegistry().registerGlobal(EventPriority.FIRST, PlayerConnectEvent.class, this::handlePlayerConnectEvent);
        // EventPriority.LAST, so whatever another listener does on the way out is written too
        plugin.getEventRegistry().registerGlobal(EventPriority.LAST, PlayerDisconnectEvent.class, this::handlePlayerDisconnectEvent);
    }

    public static QuestPlayerStateService get() {
        return OpenQuestsCorePlugin.get().getQuestPlayerStateService();
    }

    /**
     * Reads a player back: their record first, then every quest it names, in one go.
     */
    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        UUID playerId = playerConnectEvent.getPlayerRef().getUuid();
        Holder<EntityStore> holder = playerConnectEvent.getHolder();

        PlayerQuestRecord record = storage.loadPlayer(playerId);

        List<AbstractQuestProgression<?>> held = progressionStore.loadForPlayer(playerId);

        Set<UUID> resolved = new HashSet<>(held.size());
        for (AbstractQuestProgression<?> quest : held) {
            resolved.add(quest.getId());
        }

        if (resolved.size() != record.getQuestIds().size()) {
            LOGGER.atInfo().log("Player %s: %d of the %d quests on record answered", playerId, resolved.size(), record.getQuestIds().size());
        }

        QuestStoreComponent questStore = holder.ensureAndGetComponent(QuestStoreComponent.getComponentType());
        questStore.restore(resolved, record.getStartedOnConnection());

        PendingRewardStoreComponent rewards = holder.ensureAndGetComponent(PendingRewardStoreComponent.getComponentType());
        rewards.pending.restore(record.getPendingRewards());

        sessions.put(playerId, new Session(questStore, rewards));
    }

    /**
     * Writes the player out and gives back what their session brought in, without which the store
     * only grows. Dispatched more than once for one departure, so the session is taken first and
     * the rest runs for whoever got it.
     */
    private void handlePlayerDisconnectEvent(@Nonnull PlayerDisconnectEvent playerDisconnectEvent) {
        UUID playerId = playerDisconnectEvent.getPlayerRef().getUuid();

        Session session = sessions.remove(playerId);
        if (session == null) return;

        save(playerId, session, true);

        for (UUID questId : session.questStore().getQuestIds()) {
            unloadIfUnheld(questId, playerId);
        }
    }

    /**
     * Writes out every player currently on the server, for the save pass and for shutdown.
     */
    public void saveAllOnline(boolean force) {
        sessions.forEach((playerId, session) -> save(playerId, session, force));
    }

    /**
     * Writes a player record and the quests it names together: written apart, one can name a quest
     * the other never stored.
     *
     * @param force writes the record even if nothing changed, for the last write of a session.
     */
    private void save(@Nonnull UUID playerId, @Nonnull Session session, boolean force) {
        QuestStoreComponent questStore = session.questStore();
        PendingRewardStore rewards = session.rewards().pending;

        Set<UUID> questIds = new HashSet<>();

        for (UUID questId : questStore.getQuestIds()) {
            AbstractQuestProgression<?> quest = progressionStore.get(questId);

            // A quest asking not to be kept would leave an id resolving to nothing next session
            if (quest != null && !QuestProgressionStore.isPersisted(quest)) continue;

            questIds.add(questId);
        }

        progressionStore.save(progressionStore.resolveAll(questIds));

        // Cleared only after the write, so one that fails leaves the player owed another pass
        boolean changed = questStore.hasChanges() || rewards.hasChanges();
        if (!changed && !force) return;

        storage.savePlayer(playerId, new PlayerQuestRecord(questIds, questStore.getStartedOnConnection(), rewards.snapshot()));

        questStore.consumeChanges();
        rewards.consumeChanges();
    }

    /**
     * Writes down what an offline player is owed. Nothing holds their components while they are
     * away, so the storage is the only place a debt can wait for them.
     */
    public void addPendingRewards(@Nonnull UUID playerId, @Nonnull PendingRewards owed) {
        storage.addPendingRewards(playerId, owed);
    }

    /**
     * Keeps a quest someone else is still playing, and lets the rest go.
     */
    private void unloadIfUnheld(@Nonnull UUID questId, @Nonnull UUID leavingPlayerId) {
        AbstractQuestProgression<?> quest = progressionStore.get(questId);
        if (quest == null) return;

        if (isAnyOnline(quest.getPlayers(), leavingPlayerId)) return;
        if (isAnyOnline(quest.getAbandonedPlayers(), leavingPlayerId)) return;

        progressionStore.unload(questId);
    }

    /**
     * @param leavingPlayerId discounted, being still listed among the online while their own
     * departure is announced.
     */
    private static boolean isAnyOnline(@Nonnull Set<UUID> playerIds, @Nonnull UUID leavingPlayerId) {
        for (UUID playerId : playerIds) {
            if (!playerId.equals(leavingPlayerId) && Universe.get().getPlayer(playerId) != null) return true;
        }

        return false;
    }

    /**
     * What one player's session holds, as the components themselves rather than a way back to them.
     */
    private record Session(@Nonnull QuestStoreComponent questStore, @Nonnull PendingRewardStoreComponent rewards) {}
}
