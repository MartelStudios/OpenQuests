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
import com.martelstudios.openquests.core.OpenQuestsCorePlugin;
import com.martelstudios.openquests.core.assignments.repeat.AssignmentHistory;
import com.martelstudios.openquests.core.assignments.repeat.AssignmentRepeat;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.models.OpenQuestAsset;
import com.martelstudios.openquests.core.models.QuestOrigin;
import com.martelstudios.openquests.core.models.QuestState;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestScope;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;
import com.martelstudios.openquests.core.scopes.world.WorldQuestScope;
import com.martelstudios.openquests.core.scopes.world.WorldsQuestScope;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.visitors.SetStateVisitor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Hands out what the {@link OpenQuestAssignment} assets ask for. Each part of an assignment does
 * its share: the trigger recognises an occasion, the scope reaches its holders, the repeat weighs
 * what was handed before; this only carries the decision out.
 */
public class QuestAssignmentService {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * How often the triggers keeping time are asked where they stand, which bounds how late a
     * period is handed out after it begins.
     */
    private static final long TICK_SECONDS = 30;

    @Nonnull
    private final QuestStorage storage;

    /**
     * The period each holder was last found settled for, by holder key and then by assignment and
     * quest, so a period reached again and again costs no read once it is handed out.
     */
    private final Map<String, Map<String, String>> settled = new ConcurrentHashMap<>();

    public QuestAssignmentService(@Nonnull JavaPlugin plugin, @Nonnull QuestStorage storage) {
        this.storage = storage;

        plugin.getEventRegistry().registerGlobal(PlayerConnectEvent.class, this::handlePlayerConnectEvent);
        plugin.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::handleAddPlayerToWorldEvent);
        plugin.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> settled.remove(PlayerAssignmentHolder.keyOf(event.getPlayerRef().getUuid())));
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
     * Brings an occasion to the assignment's scope, which reaches the holders it concerns.
     */
    public void evaluate(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion) {
        assignment.getScope().reach(assignment, occasion);
    }

    /**
     * Offers every quest the assignment lists to one holder, on the thread allowed to touch it.
     *
     * @param occasion the key telling this hand-out from the others, as the scope reads it
     * @param timed whether the occasion is a period, which comes back each time the holder is reached
     */
    public void offerAll(@Nonnull OpenQuestAssignment assignment, @Nonnull AssignmentHolder holder, @Nonnull String occasion, boolean timed) {
        for (String questAssetId : assignment.getQuestAssetIds()) {
            OpenQuestAsset asset = OpenQuestAsset.getAsset(questAssetId);
            if (asset == null) continue;

            if (offer(assignment, asset, holder, occasion, timed) && timed) {
                settled.computeIfAbsent(holder.getKey(), key -> new ConcurrentHashMap<>()).put(assignment.getId() + "/" + questAssetId, occasion);
            }
        }
    }

    /**
     * Only ever true for what this server saw handed out: another server handing a period out
     * first is found on the next read instead.
     *
     * @return whether every quest the assignment lists was already settled with that holder for
     * that occasion.
     */
    public boolean isSettled(@Nonnull String holderKey, @Nonnull OpenQuestAssignment assignment, @Nonnull String occasion) {
        Map<String, String> byQuest = settled.get(holderKey);
        if (byQuest == null) return false;

        for (String questAssetId : assignment.getQuestAssetIds()) {
            if (!occasion.equals(byQuest.get(assignment.getId() + "/" + questAssetId))) return false;
        }
        return true;
    }

    /**
     * @param player the player's components, written on the thread the occasion came on
     * @return the player as a holder, their records travelling with their own.
     */
    @Nonnull
    public AssignmentHolder playerHolder(@Nonnull UUID playerId, @Nonnull EntityComponents player) {
        return new PlayerAssignmentHolder(playerId, player.ensureAndGetComponent(QuestStoreComponent.getComponentType()));
    }

    /**
     * @param joining the player entering that world, who is not among its players until the entry
     * is done, so a quest created on their way in is handed to them here
     * @return the world as a holder, its records in the shared store under its index key.
     */
    @Nonnull
    public AssignmentHolder worldHolder(@Nonnull World world, @Nullable UUID joining) {
        return new SharedAssignmentHolder(storage, WorldQuestService.indexKey(world), new WorldQuestScope(List.of(world.getWorldConfig().getUuid())), joining);
    }

    /**
     * @param joining the player still connecting, not online yet for the server to hand a quest
     * created now to them
     * @return the server as a holder, its records in the shared store under its index key.
     */
    @Nonnull
    public AssignmentHolder universeHolder(@Nullable UUID joining) {
        return new SharedAssignmentHolder(storage, UniverseQuestService.UNIVERSE_INDEX_KEY, UniverseQuestScope.INSTANCE, joining);
    }

    private void tick() {
        try {
            Instant now = Instant.now();

            for (OpenQuestAssignment assignment : List.copyOf(OpenQuestAssignment.getAssetMap().getAssetMap().values())) {
                Occasion occasion = assignment.getTrigger().onTick(now);
                if (occasion != null) evaluate(assignment, occasion);
            }
        } catch (RuntimeException e) {
            // Thrown out of a scheduled task, it would cancel every tick after it
            LOGGER.atWarning().withCause(e).log("Handing out scheduled quests failed");
        }
    }

    /**
     * @param joining the player entering a world of the group, who is not among its players until
     * the entry is done
     * @return the group of worlds as a holder, its records and index under the group's key.
     */
    @Nonnull
    public AssignmentHolder groupHolder(@Nonnull String group, @Nullable UUID joining) {
        return new SharedAssignmentHolder(storage, WorldGroupIndex.keyOf(group), new WorldsQuestScope(group), joining);
    }

    /**
     * Through the incoming holder: the player is not online yet, and what is written to their
     * stored data would be overwritten.
     */
    private void handlePlayerConnectEvent(@Nonnull PlayerConnectEvent playerConnectEvent) {
        UUID playerId = playerConnectEvent.getPlayerRef().getUuid();
        EntityComponents player = EntityComponents.of(playerConnectEvent.getHolder());

        for (OpenQuestAssignment assignment : List.copyOf(OpenQuestAssignment.getAssetMap().getAssetMap().values())) {
            Occasion occasion = assignment.getTrigger().onConnect(playerId, player);
            if (occasion != null) evaluate(assignment, occasion);
        }
    }

    private void handleAddPlayerToWorldEvent(@Nonnull AddPlayerToWorldEvent addPlayerToWorldEvent) {
        PlayerRef playerRef = addPlayerToWorldEvent.getHolder().getComponent(PlayerRef.getComponentType());
        if (playerRef == null) return;

        EntityComponents player = EntityComponents.of(addPlayerToWorldEvent.getHolder());
        World world = addPlayerToWorldEvent.getWorld();

        for (OpenQuestAssignment assignment : List.copyOf(OpenQuestAssignment.getAssetMap().getAssetMap().values())) {
            assignment.getScope().onEnterWorld(assignment, playerRef.getUuid(), world);

            Occasion occasion = assignment.getTrigger().onEnterWorld(playerRef.getUuid(), player, world);
            if (occasion != null) evaluate(assignment, occasion);
        }
    }

    /**
     * Lets the repeat decide on what this holder already had, and carries it out. A replaced line
     * fails only once the new quest is out, so a refused hand-out leaves it be.
     *
     * @return whether the holder now has this occasion handed out, by this call or an earlier one.
     */
    private boolean offer(@Nonnull OpenQuestAssignment assignment, @Nonnull OpenQuestAsset asset, @Nonnull AssignmentHolder holder, @Nonnull String occasion, boolean timed) {
        String assignmentId = assignment.getId();
        String questAssetId = asset.getId();

        AssignmentRecord record = holder.getRecord(assignmentId, questAssetId);
        AssignmentHistory history = new AssignmentHistory(record, occasion, timed, () -> readLine(holder, assignmentId, questAssetId, occasion));

        AssignmentRepeat.Decision decision = assignment.getRepeat().decide(history);
        if (decision == AssignmentRepeat.Decision.SKIP) return history.isHandedAlready();

        // Read before the new quest is out, which would otherwise count as part of the line
        List<UUID> replaced = decision == AssignmentRepeat.Decision.REPLACE ? history.getRunningLine() : List.of();

        AbstractQuestProgression<?> quest = asset.create();
        quest.setOrigin(new QuestOrigin(assignmentId, questAssetId, occasion));

        if (!holder.handOut(quest, assignmentId, questAssetId, record, AssignmentRecord.next(record, occasion, Instant.now()))) return false;

        if (!replaced.isEmpty()) QuestProgressionService.get().progress(new SetStateVisitor(QuestState.FAILED), replaced);
        return true;
    }

    /**
     * Everything one occasion opened carries its origin down the chain, so the line is found on
     * the holder's quests without following any chain.
     */
    @Nonnull
    private static AssignmentHistory.Line readLine(@Nonnull AssignmentHolder holder, @Nonnull String assignmentId, @Nonnull String questAssetId, @Nonnull String occasion) {
        boolean openedOnOccasion = false;
        List<UUID> running = new ArrayList<>();

        for (AbstractQuestProgression<?> held : holder.getQuests()) {
            QuestOrigin origin = held.getOrigin();
            if (origin == null || !origin.isLineOf(assignmentId, questAssetId)) continue;

            if (occasion.equals(origin.getOccasion())) openedOnOccasion = true;
            if (holder.isRunning(held)) running.add(held.getId());
        }
        return new AssignmentHistory.Line(openedOnOccasion, running);
    }
}
