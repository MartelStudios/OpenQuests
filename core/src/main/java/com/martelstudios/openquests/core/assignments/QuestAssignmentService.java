package com.martelstudios.openquests.core.assignments;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.RemoveWorldEvent;
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Hands out what the {@link OpenQuestAssignment} assets ask for. Each part of an assignment does
 * its share: the trigger recognises an occasion, the scope picks its holders, the repeat weighs
 * what was handed before. This brings occasions to them and reaches the holders picked.
 */
public class QuestAssignmentService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * How often the triggers keeping time are asked where they stand, which bounds how late a
     * period is handed out after it begins.
     */
    private static final long TICK_SECONDS = 30;

    @Nonnull
    private final SharedAssignmentRecords records;

    @Nonnull
    private final AssignmentHolders holders;

    private final AssignmentOffer offer = new AssignmentOffer();

    private final SettledOccasions settled = new SettledOccasions();

    public QuestAssignmentService(@Nonnull JavaPlugin plugin, @Nonnull QuestStorage storage) {
        this.records = new SharedAssignmentRecords(storage);
        this.holders = new AssignmentHolders(records);

        plugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::handlePlayerConnectEvent);
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::handleAddPlayerToWorldEvent);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> settled.forget(AssignmentHolders.playerKey(event.getPlayerRef().getUuid())));
        plugin.getEventRegistry().registerGlobal(RemoveWorldEvent.class, this::handleRemoveWorldEvent);
    }

    public static QuestAssignmentService get() {
        return OpenQuestsCorePlugin.get().getQuestAssignmentService();
    }

    /**
     * Starts asking the triggers that keep time where they stand, soon and then every so often: a
     * period that began while the server was down is handed out as it comes back.
     */
    public void start() {
        HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(this::tick, 5, TICK_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Brings an occasion to the assignment's scope, which picks the holders it concerns.
     */
    public void evaluate(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion) {
        assignment.getScope().reach(assignment, occasion, new Reach(assignment, occasion));
    }

    private void tick() {
        try {
            Instant now = Instant.now();

            forEachAssignment("the schedule", assignment -> {
                Occasion occasion = assignment.getTrigger().onTick(now);
                if (occasion != null) evaluate(assignment, occasion);
            });
        } catch (Throwable e) {
            // Anything thrown out of a scheduled task cancels every tick after it, silently
            LOGGER.atWarning().withCause(e).log("Handing out scheduled quests failed");
        }
    }

    /**
     * Through the incoming holder: the player is not online yet, and what is written to their
     * stored data would be overwritten.
     */
    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        UUID playerId = playerConnectEvent.getPlayerRef().getUuid();
        EntityComponents player = EntityComponents.of(playerConnectEvent.getHolder());

        forEachAssignment("a connection", assignment -> {
            Occasion occasion = assignment.getTrigger().onConnect(playerId, player);
            if (occasion != null) evaluate(assignment, occasion);
        });
    }

    /**
     * The group a world belongs to takes its running quests up first, so that an occasion the
     * entry is finds the world already in its group.
     */
    private void handleAddPlayerToWorldEvent(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        PlayerRef playerRef = addPlayerToWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        EntityComponents player = EntityComponents.of(addPlayerToWorldEvent.getHolder());
        World world = addPlayerToWorldEvent.getWorld();

        forEachAssignment("a world entry", assignment -> {
            String group = assignment.getScope().groupOf(assignment, world);
            if (group != null) WorldGroupIndex.get().enter(group, world, playerRef.getUuid());

            Occasion occasion = assignment.getTrigger().onEnterWorld(playerRef.getUuid(), player, world);
            if (occasion != null) evaluate(assignment, occasion);
        });
    }

    /**
     * A world closing for good takes its records with it, deleted from the store as it closes.
     */
    private void handleRemoveWorldEvent(@Nonnull RemoveWorldEvent removeWorldEvent) {
        String key = AssignmentHolders.worldKey(removeWorldEvent.getWorld());
        settled.forget(key);
        records.forget(key);
    }

    /**
     * Each assignment on its own: one that throws, a storage that failed, leaves the others to
     * run, and never reaches the event it was answering.
     */
    private void forEachAssignment(@Nonnull String occasion, @Nonnull Consumer<OpenQuestAssignment> action) {
        for (OpenQuestAssignment assignment : List.copyOf(OpenQuestAssignment.getAssetMap().getAssetMap().values())) {
            try {
                action.accept(assignment);
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("Assignment %s failed on %s", assignment.getId(), occasion);
            }
        }
    }

    /**
     * One occasion of one assignment, reaching the holders its scope picks: each on the thread
     * allowed to touch it, and none settled already for the period. A holder that moves, a player,
     * tells hand-outs apart by place and time; the others by time alone.
     */
    private final class Reach implements AssignmentTargets {

        @Nonnull
        private final OpenQuestAssignment assignment;

        @Nonnull
        private final Occasion occasion;

        private Reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion) {
            this.assignment = assignment;
            this.occasion = occasion;
        }

        @Override
        public void player(@Nonnull UUID playerId, @Nonnull EntityComponents player) {
            String key = occasion.placeKey();
            if (isSettled(AssignmentHolders.playerKey(playerId), key)) return;

            offerAll(holders.player(playerId, player), key);
        }

        @Override
        public void onlinePlayers() {
            String key = occasion.placeKey();

            for (PlayerRef playerRef : Universe.get().getPlayers()) {
                UUID playerId = playerRef.getUuid();
                if (isSettled(AssignmentHolders.playerKey(playerId), key)) continue;

                EntityComponents.update(playerId, player -> offerAll(holders.player(playerId, player), key));
            }
        }

        @Override
        public void world(@Nonnull World world, @Nullable UUID joining) {
            String key = occasion.timeKey();
            if (isSettled(AssignmentHolders.worldKey(world), key)) return;

            WorldQuestService.onThreadOf(world, () -> offerAll(holders.world(world, joining), key));
        }

        @Override
        public void group(@Nonnull String group, @Nullable UUID joining) {
            String key = occasion.timeKey();
            if (isSettled(AssignmentHolders.groupKey(group), key)) return;

            offerAll(holders.group(group, joining), key);
        }

        @Override
        public void universe(@Nullable UUID joining) {
            String key = occasion.timeKey();
            if (isSettled(AssignmentHolders.universeKey(), key)) return;

            offerAll(holders.universe(joining), key);
        }

        private boolean isSettled(@Nonnull String holderKey, @Nonnull String key) {
            return occasion.isTimed() && settled.isSettled(holderKey, assignment, key);
        }

        /**
         * Often run later, on another thread, where nothing would report what it throws.
         */
        private void offerAll(@Nonnull AssignmentHolder holder, @Nonnull String key) {
            try {
                for (String questAssetId : assignment.getQuestAssetIds()) {
                    OpenQuestAsset asset = OpenQuestAsset.getAsset(questAssetId);
                    if (asset == null) continue;

                    if (offer.offer(assignment, asset, holder, key, occasion.isTimed()) && occasion.isTimed()) {
                        settled.settle(holder.getKey(), assignment.getId(), questAssetId, key);
                    }
                }
            } catch (RuntimeException e) {
                LOGGER.atWarning().withCause(e).log("Assignment %s failed to reach %s", assignment.getId(), holder.getKey());
            }
        }
    }
}
