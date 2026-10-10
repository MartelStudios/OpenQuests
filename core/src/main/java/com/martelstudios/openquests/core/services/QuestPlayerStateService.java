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
import com.martelstudios.openquests.core.events.QuestCompletedEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestCompletions;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.PlayerMessage;
import com.martelstudios.openquests.core.persistence.PlayerQuestRecord;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.rewards.models.PendingRewards;
import com.martelstudios.openquests.core.rewards.services.QuestRewardService;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStore;
import com.martelstudios.openquests.core.rewards.stores.PendingRewardStoreComponent;
import com.martelstudios.openquests.core.stores.QuestProgressionStore;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;

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
        plugin.getEventRegistry().registerGlobal(QuestCompletedEvent.class, this::handleQuestCompletedEvent);
    }

    public static QuestPlayerStateService get() {
        return OpenQuestsCorePlugin.get().getQuestPlayerStateService();
    }

    /**
     * Reads a player back: their record first, then every quest it names, in one go. Hosting them
     * first, which waits for the server they come from to have written them out.
     */
    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        UUID playerId = playerConnectEvent.getPlayerRef().getUuid();
        Holder<EntityStore> holder = playerConnectEvent.getHolder();

        PlayerQuestRecord record = storage.hostPlayer(playerId);

        // What other servers left them while they were away, let go of once the record holding it is written
        Set<UUID> delivered = ConcurrentHashMap.newKeySet();
        for (PlayerMessage message : storage.loadMessages(List.of(playerId))) {
            message.applyTo(record);
            delivered.add(message.getId());
        }

        List<AbstractQuestProgression<?>> held = progressionStore.loadForPlayer(playerId);

        Set<UUID> resolved = new HashSet<>(held.size());
        for (AbstractQuestProgression<?> quest : held) {
            resolved.add(quest.getId());
        }

        if (resolved.size() != record.getQuestIds().size()) {
            LOGGER.atInfo().log("Player %s: %d of the %d quests on record answered", playerId, resolved.size(), record.getQuestIds().size());
        }

        // Kept on record, so that a quest set aside comes back with its asset rather than for good
        for (UUID questId : record.getQuestIds()) {
            if (progressionStore.isSetAside(questId)) resolved.add(questId);
        }

        QuestStoreComponent questStore = holder.ensureAndGetComponent(QuestStoreComponent.getComponentType());
        questStore.restore(resolved, record.getAssignments(), record.getCompletions());

        PendingRewardStoreComponent rewards = holder.ensureAndGetComponent(PendingRewardStoreComponent.getComponentType());
        rewards.pending.restore(record.getPendingRewards());

        sessions.put(playerId, new Session(questStore, rewards, delivered));
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

        save(playerId, session, true, true);

        for (UUID questId : session.questStore().getQuestIds()) {
            unloadIfUnheld(questId, playerId);
        }
    }

    /**
     * Writes out every player currently on the server, for the save pass and for shutdown.
     *
     * @param stopping whether the server is stopping, which ends every stay here
     */
    public void saveAllOnline(boolean stopping) {
        sessions.forEach((playerId, session) -> save(playerId, session, stopping, stopping));
    }

    /**
     * Writes a player record and the quests it names together: written apart, one can name a quest
     * the other never stored.
     *
     * @param force writes the record even if nothing changed, for the last write of a session.
     * @param leaving whether their stay here ends with this write, letting another server host them
     */
    private void save(@Nonnull UUID playerId, @Nonnull Session session, boolean force, boolean leaving) {
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
        Set<UUID> delivered = Set.copyOf(session.delivered());
        boolean changed = questStore.hasChanges() || rewards.hasChanges() || !delivered.isEmpty();
        if (!changed && !force) return;

        storage.savePlayer(playerId, new PlayerQuestRecord(questIds, questStore.getAssignments().snapshot(), rewards.snapshot(), questStore.getCompletions()), delivered, leaving);
        session.delivered().removeAll(delivered);

        questStore.consumeChanges();
        rewards.consumeChanges();
    }

    /**
     * Leaves a debt for a player this server does not host: whichever server hosts them, now or
     * next, takes it into their record. Their record is never written from here.
     */
    public void addPendingRewards(@Nonnull UUID playerId, @Nonnull PendingRewards owed) {
        storage.postMessage(PlayerMessage.owed(playerId, owed));
    }

    /**
     * Takes in what other servers left the players this server hosts, each message once: a debt is
     * owed, and paid at once where it claims itself; an ending is counted. The next write of the
     * player's record lets go of them.
     */
    public void deliverMessages(@Nonnull List<PlayerMessage> messages) {
        for (PlayerMessage message : messages) {
            Session session = sessions.get(message.getPlayerId());
            if (session == null || !session.delivered().add(message.getId())) continue;

            // Into the component held here, so the debt is written with the record even if the player
            // leaves before it is paid; then paid on their thread where it claims itself
            if (message.getOwed() != null) {
                session.rewards().pending.add(message.getOwed());
                EntityComponents.update(message.getPlayerId(), QuestRewardService.get()::claimAuto);
            }

            PlayerMessage.Ending ending = message.getEnding();
            if (ending != null) session.questStore().recordCompletion(ending.getAssetId(), ending.getOutcome(), ending.startedAt(), ending.completedAt());
        }
    }

    /**
     * @return the players this server hosts, whose messages it takes in.
     */
    @Nonnull
    public Set<UUID> getHostedPlayers() {
        return Set.copyOf(sessions.keySet());
    }

    /**
     * @return how quests from that asset ended for the player so far. An offline player is read
     * from the storage, which blocks: meant for the rare question asked about someone away.
     */
    @Nonnull
    public QuestCompletions getCompletions(@Nonnull UUID playerId, @Nonnull String assetId) {
        Session session = sessions.get(playerId);
        if (session != null) return session.questStore().getCompletions(assetId);

        return storage.loadPlayer(playerId).getCompletions().getOrDefault(assetId, QuestCompletions.NONE);
    }

    /**
     * Counts the outcome for everyone holding the quest as it ends, those who gave it up as having
     * abandoned it. Counted here rather than as they leave, so a player who comes back and sees
     * it through counts once, the way it ended for them. Counted by the server that
     * ended it alone, so that every server learning of the end does not count it again.
     */
    private void handleQuestCompletedEvent(@Nonnull QuestCompletedEvent questCompletedEvent) {
        if (!questCompletedEvent.isEndedHere()) return;

        AbstractQuestProgression<?> quest = questCompletedEvent.getQuest();

        String assetId = quest.getAssetId();
        if (assetId == null) return;

        for (UUID playerId : quest.getPlayers()) {
            recordCompletion(playerId, assetId, questCompletedEvent.getState(), quest);
        }

        for (UUID playerId : quest.getAbandonedPlayers()) {
            recordCompletion(playerId, assetId, QuestState.ABANDONED, quest);
        }
    }

    /**
     * Into the session of a player this server hosts, written with the rest of their record; left
     * as a message for one it does not.
     */
    private void recordCompletion(@Nonnull UUID playerId, @Nonnull String assetId, @Nonnull QuestState outcome, @Nonnull AbstractQuestProgression<?> quest) {
        Session session = sessions.get(playerId);
        if (session != null) {
            session.questStore().recordCompletion(assetId, outcome, quest.getStartedAt(), quest.getCompletedAt());
            return;
        }

        storage.postMessage(PlayerMessage.ended(playerId, assetId, outcome, quest.getStartedAt(), quest.getCompletedAt()));
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
     * What one player's session holds, as the components themselves rather than a way back to them,
     * and the messages taken into them that the next write lets go of.
     */
    private record Session(@Nonnull QuestStoreComponent questStore, @Nonnull PendingRewardStoreComponent rewards, @Nonnull Set<UUID> delivered) {}
}
