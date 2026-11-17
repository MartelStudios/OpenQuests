package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.event.IEventBus;
import com.hypixel.hytale.event.IEventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.events.TeamJoinedEvent;
import com.martelstudios.openquests.core.events.TeamLeftEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AssignmentRecord;
import com.martelstudios.openquests.core.persistence.PlayerMessage;
import com.martelstudios.openquests.core.persistence.QuestProgressionRecord;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.persistence.TeamTransaction;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.team.TeamQuestScope;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.stores.PlayerComponentTypes;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.sync.QuestSyncService;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.utils.Players;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Commits membership once, then recovers quest effects and updates the hosted player's cache. */
public class TeamService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Teams teams;
    private final TeamQuestService teamQuests;
    private final TeamTypes types;
    private final PlayerComponentTypes components;
    private final QuestPlayerStateService playerState;
    private final Players players;
    private final ScopeIndexes indexes;
    private final QuestAssignmentService assignments;
    private final QuestCopier copier;
    private final IEventBus events;
    private final QuestStorage storage;
    private final QuestSyncService sync;

    public TeamService(@Nonnull Teams teams, @Nonnull TeamQuestService teamQuests, @Nonnull TeamTypes types, @Nonnull PlayerComponentTypes components,
                       @Nonnull QuestPlayerStateService playerState, @Nonnull Players players, @Nonnull ScopeIndexes indexes, @Nonnull QuestAssignmentService assignments,
                       @Nonnull QuestCopier copier, @Nonnull IEventBus events, @Nonnull QuestStorage storage, @Nonnull QuestSyncService sync) {
        this.teams = teams;
        this.teamQuests = teamQuests;
        this.types = types;
        this.components = components;
        this.playerState = playerState;
        this.players = players;
        this.indexes = indexes;
        this.assignments = assignments;
        this.copier = copier;
        this.events = events;
        this.storage = storage;
        this.sync = sync;
    }

    /** @return the personal team whose running progress would be discarded by joining another. */
    @Nullable
    public TeamRecord wouldLose(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull String type) {
        return wouldLose(playerId, type);
    }

    /** @return the current authoritative membership, independently of the server hosting the player. */
    @Nullable
    public UUID currentTeamOf(@Nonnull UUID playerId, @Nonnull String type) {
        return storage.loadTeamsForPlayer(playerId).stream().filter(team -> team.getType().equals(type))
                .map(TeamRecord::getId).findFirst().orElse(null);
    }

    /** @return what joining would discard, or null when nobody loses running progress. */
    @Nullable
    public TeamRecord wouldLose(@Nonnull UUID playerId, @Nonnull String type) {
        UUID id = currentTeamOf(playerId, type);
        TeamRecord team = id == null ? null : teams.get(id);
        return team != null && team.getMembers().equals(List.of(playerId)) && !teamQuests.getRunningQuests(id).isEmpty() ? team : null;
    }

    /** Runs on a storage thread; joining keeps the destination's history and discards the old personal team's. */
    public void join(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull UUID teamId) {
        TeamRecord target = teams.get(teamId);
        if (target == null) throw new IllegalArgumentException("No team " + teamId);
        PlayerMessage message = move(playerId, "", target.getType(), teamId, false);
        if (message != null) {
            prepare(playerId, message.getTeam());
            apply(playerId, player, message.getTeam());
        }
    }

    /** Runs on a storage thread; a personal departure snapshots progress before changing membership. */
    public void leave(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull EntityComponents player, @Nonnull String type) {
        PlayerMessage message = move(playerId, playerName, type, null, false);
        if (message != null) {
            prepare(playerId, message.getTeam());
            apply(playerId, player, message.getTeam());
        }
    }

    /** Performs a durable move; the notification remains available if quest effects fail afterwards. */
    public void change(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull String type, @Nullable UUID to) {
        PlayerMessage message = move(playerId, playerName, type, to, false);
        if (message == null) return;
        prepare(playerId, message.getTeam());
        if (players.isOnline(playerId)) players.update(playerId, player -> apply(playerId, player, message.getTeam()));
    }

    /** Completes only after the durable move and its quest effects succeed; failures reach the command caller. */
    @Nonnull
    public CompletableFuture<Void> changeAsync(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull String type, @Nullable UUID to) {
        return sync.submit(() -> change(playerId, playerName, type, to));
    }

    /** Runs a command's reads, validation and writes together off the world thread. */
    @Nonnull
    public CompletableFuture<Void> execute(@Nonnull Runnable command) { return sync.submit(command); }

    /** Creates and joins the destination in the same commit, so a crash cannot leave a spare membership. */
    @Nonnull
    public TeamRecord create(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull String type, @Nullable String name) {
        Teams.validateName(name);
        TeamRecord created = new TeamRecord(UUID.randomUUID(), type, name, playerName, playerId, List.of(playerId), List.of());
        PlayerMessage message = move(playerId, playerName, type, created.getId(), false, created, false);
        complete(playerId, message);
        return created;
    }

    /** Accepts only an invitation still valid in the same locked snapshot that changes membership. */
    public void accept(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull String type, @Nonnull UUID destination) {
        complete(playerId, move(playerId, playerName, type, destination, false, null, true));
    }

    private void complete(UUID playerId, PlayerMessage message) {
        if (message == null) return;
        prepare(playerId, message.getTeam());
        if (players.isOnline(playerId)) players.update(playerId, player -> apply(playerId, player, message.getTeam()));
    }

    private PlayerMessage move(UUID playerId, String playerName, String type, UUID requested, boolean ensurePersonal) {
        return move(playerId, playerName, type, requested, ensurePersonal, null, false);
    }

    private PlayerMessage move(UUID playerId, String playerName, String type, UUID requested, boolean ensurePersonal,
                               TeamRecord created, boolean requireInvitation) {
        OpenQuestTeamType definition = types.get(type);
        if (definition == null) throw new IllegalArgumentException("Unknown team type " + type);
        return storage.transactTeams(playerId, requested, all -> {
            List<TeamRecord> sources = all.stream().filter(team -> team.getType().equals(type) && team.getMembers().contains(playerId)).toList();
            if (ensurePersonal && !sources.isEmpty()) return new TeamTransaction(List.of(), null);
            if (!ensurePersonal && requested == null && (sources.isEmpty() || definition.isPersonalTeam() && sources.size() == 1
                    && sources.getFirst().getMembers().equals(List.of(playerId)))) return new TeamTransaction(List.of(), null);
            TeamRecord target = requested == null ? null : all.stream().filter(team -> team.getId().equals(requested)).findFirst().orElse(created);
            if (requested != null && target == null) throw new IllegalArgumentException("No team " + requested);
            if (target != null && (!target.getType().equals(type) || target.getMembers().isEmpty()))
                throw new IllegalArgumentException("Gone or wrong-type team " + requested);
            if (requireInvitation && !teams.hasInvitation(target, playerId)) throw new IllegalArgumentException("No current invitation");
            if (target != null && sources.size() == 1 && sources.getFirst().getId().equals(target.getId()))
                return new TeamTransaction(List.of(), null);

            List<QuestProgressionRecord> copies = new ArrayList<>();
            Map<String, Map<String, AssignmentRecord>> history = new HashMap<>();
            if (requested == null && definition.isPersonalTeam()) {
                target = new TeamRecord(UUID.randomUUID(), type, null, playerName, playerId, List.of(playerId), List.of());
                // Preparation has no registrations or writes: failure leaves both memberships untouched.
                for (TeamRecord source : sources) {
                    for (AbstractQuestProgression<?> quest : teamQuests.getRunningQuests(source.getId())) {
                        if (quest.getPlayers().contains(playerId)) copier.snapshot(quest, playerId).forEach(copy -> copies.add(new QuestProgressionRecord(copy)));
                    }
                    storage.loadAssignments(TeamQuestService.key(source.getId())).snapshot().forEach((assignment, records) ->
                            history.computeIfAbsent(assignment, ignored -> new HashMap<>()).putAll(records));
                }
            }
            UUID destination = target == null ? null : target.getId();
            List<TeamRecord> writes = new ArrayList<>();
            List<UUID> left = new ArrayList<>();
            for (TeamRecord source : sources) {
                if (source.getId().equals(destination)) continue;
                List<UUID> members = new ArrayList<>(source.getMembers());
                members.remove(playerId);
                UUID owner = source.getOwner().equals(playerId) && !members.isEmpty() ? members.getFirst() : source.getOwner();
                writes.add(source.withMembers(members).withOwner(owner).withVersion(source.getVersion() + 1));
                left.add(source.getId());
            }
            if (target != null) {
                List<UUID> members = new ArrayList<>(target.getMembers());
                if (!members.contains(playerId)) members.add(playerId);
                boolean existed = all.stream().anyMatch(team -> team.getId().equals(destination));
                writes.add(target.withMembers(members).withInvitations(target.getInvitations().stream()
                        .filter(invite -> !invite.player().equals(playerId)).toList()).withVersion(existed ? target.getVersion() + 1 : 0));
            }
            TeamTransaction transaction = new TeamTransaction(writes, PlayerMessage.team(playerId, new TeamChange(type, left, destination, copies, history)));
            if (destination != null) transaction.removeFromIndex(Teams.invitationsKey(playerId), destination);
            return transaction;
        });
    }

    /** Runs on the storage worker before any component update; replay never generates new copy ids. */
    public void prepare(@Nonnull UUID playerId, @Nonnull TeamChange change) {
        UUID current = currentTeamOf(playerId, change.type());
        // A later move supersedes this notification. It must not resurrect an old personal team.
        boolean currentMove = Objects.equals(current, change.to());
        if (currentMove && current != null) {
            List<AbstractQuestProgression<?>> roots = new ArrayList<>();
            for (QuestProgressionRecord snapshot : change.copies()) {
                AbstractQuestProgression<?> copy = snapshot.quest;
                if (copy.getParentId() == null) {
                    copy.apply(new AbstractQuestProgression.SetScope(new TeamQuestScope(current)));
                }
                AbstractQuestProgression<?> persisted = storage.loadProgression(copy.getId());
                if (persisted == null) storage.writeProgressions(List.of(copy));
                AbstractQuestProgression<?> actual = persisted == null ? copy : persisted;
                if (actual.getParentId() == null && !actual.isOver()) roots.add(actual);
                if (playerState.getQuestStore(playerId) != null) {
                    teamQuests.restoreCopy(persisted == null ? copy : persisted);
                }
            }
            for (AbstractQuestProgression<?> root : roots) storage.addToIndex(TeamQuestService.key(current), List.of(root.getId()));
            change.assignments().forEach((assignment, records) -> records.forEach((asset, record) ->
                    storage.claimAssignment(TeamQuestService.key(current), assignment, asset, null, record)));
            assignments.forgetHolder(TeamQuestService.key(current));
        }
        for (UUID source : change.sources()) {
            if (source.equals(current)) continue;
            teamQuests.leaveRunningQuests(source, playerId);
            TeamRecord team = teams.get(source);
            if (team == null || team.getMembers().isEmpty()) {
                teamQuests.disband(source);
                assignments.deleteHolder(TeamQuestService.key(source));
                if (team != null) storage.deleteTeam(source, team.getVersion());
            }
        }
        if (currentMove && current != null) teamQuests.joinRunningQuests(current, playerId);
        Set<UUID> touched = new HashSet<>(change.sources());
        if (change.to() != null) touched.add(change.to());
        sync.execute("teams without a local member", () -> {
            Set<UUID> hosted = playerState.getHostedPlayers();
            for (UUID id : touched) {
                TeamRecord team = teams.get(id);
                if (team != null && team.getMembers().stream().anyMatch(member -> players.isOnline(member) || hosted.contains(member))) continue;
                teamQuests.unload(id);
                assignments.forgetHolder(TeamQuestService.key(id));
            }
        });
    }

    /** Updates only the hosted cache; storage work must have succeeded through prepare first. */
    public void apply(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull TeamChange change) {
        QuestStoreComponent store = player.ensureAndGetComponent(components.questStore());
        UUID held;
        synchronized (store) {
            if (!store.acceptTeamRevision(change.type(), change.revision())) return;
            held = store.getTeams().get(change.type());
            store.setTeam(change.type(), change.to());
        }
        if (held != null && !held.equals(change.to())) events.dispatchFor(TeamLeftEvent.class, playerId).dispatch(new TeamLeftEvent(playerId, held));
        if (change.to() != null && !change.to().equals(held)) events.dispatchFor(TeamJoinedEvent.class, playerId).dispatch(new TeamJoinedEvent(playerId, change.to()));
        sync.execute("the teams of player " + playerId, () -> playerState.saveNow(playerId));
    }

    /** Checks authoritative membership on the storage worker before accepting a delayed notification. */
    public boolean isCurrent(@Nonnull UUID playerId, @Nonnull TeamChange change) {
        return Objects.equals(currentTeamOf(playerId, change.type()), change.to());
    }

    /** Registers connection reconciliation, whose holder is available before world entry. */
    public void listen(@Nonnull IEventRegistry registry) {
        registry.registerGlobal(PlayerConnectEvent.class, event -> {
            UUID player = event.getPlayerRef().getUuid();
            try { onConnect(player, event.getPlayerRef().getUsername(), EntityComponents.of(event.getHolder())); }
            catch (RuntimeException e) { LOGGER.atWarning().withCause(e).log("Could not reconcile the teams of player %s", player); }
        });
        registry.registerGlobal(PlayerDisconnectEvent.class, event -> onDisconnect(event.getPlayerRef().getUuid()));
    }

    /** Releases teams nobody hosted here still plays. */
    public void onDisconnect(@Nonnull UUID playerId) {
        QuestStoreComponent leaving = playerState.getQuestStore(playerId);
        if (leaving == null) return;
        Set<UUID> held = new HashSet<>();
        for (UUID other : players.online()) {
            QuestStoreComponent store = other.equals(playerId) ? null : playerState.getQuestStore(other);
            if (store != null) held.addAll(store.getTeams().values());
        }
        for (UUID id : leaving.getTeams().values()) if (!held.contains(id)) {
            indexes.unload(TeamQuestService.key(id));
            assignments.forgetHolder(TeamQuestService.key(id));
        }
    }

    /** Rebuilds the cache from documents, recovering a crash even when the cached player record is stale. */
    public void onConnect(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull EntityComponents player) {
        Map<String, UUID> previous = new HashMap<>(player.ensureAndGetComponent(components.questStore()).getTeams());
        playerState.applyTeamChanges(playerId, player);
        QuestStoreComponent store = player.ensureAndGetComponent(components.questStore());
        for (OpenQuestTeamType type : types.all()) {
            if (type.isPersonalTeam()) {
                PlayerMessage created = move(playerId, playerName, type.getId(), null, true);
                if (created != null) { prepare(playerId, created.getTeam()); apply(playerId, player, created.getTeam()); }
            }
        }
        Map<String, UUID> actual = new HashMap<>();
        for (TeamRecord team : storage.loadTeamsForPlayer(playerId)) actual.put(team.getType(), team.getId());
        for (var entry : previous.entrySet()) {
            if (actual.containsValue(entry.getValue())) continue;
            TeamRecord old = teams.get(entry.getValue());
            if (old == null || old.getMembers().isEmpty()) {
                teamQuests.disband(entry.getValue());
                assignments.deleteHolder(TeamQuestService.key(entry.getValue()));
            }
            store.setTeam(entry.getKey(), null);
        }
        actual.forEach(store::setTeam);
        for (UUID team : actual.values()) teamQuests.joinRunningQuests(team, playerId, store);
        sync.execute("the connected player's teams", () -> playerState.saveNow(playerId));
    }
}
