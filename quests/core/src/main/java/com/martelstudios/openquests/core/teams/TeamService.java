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
import com.martelstudios.openquests.core.persistence.PlayerMessage;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.stores.PlayerComponentTypes;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.sync.QuestSyncService;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.utils.Players;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps a player's teams and their quests in step: the team documents on one side, the quests
 * the teams share on the other, and the player's record caching which teams they are in.
 */
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

    /**
     * @return the team the player would disband by joining another of that type: one they are
     * alone in with quests running, which they would lose. {@code null} when nothing is lost.
     */
    @Nullable
    public TeamRecord wouldLose(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull String type) {
        UUID current = questStore(player).getTeams().get(type);
        return current == null ? null : losing(current, playerId);
    }

    @Nullable
    private TeamRecord losing(@Nonnull UUID teamId, @Nonnull UUID playerId) {
        TeamRecord team = teams.get(teamId);
        if (team == null || !team.getMembers().equals(List.of(playerId))) return null;

        return teamQuests.getRunningQuests(teamId).isEmpty() ? null : team;
    }

    /**
     * Creates a team owned by the player, who leaves their team of that type as on a join.
     *
     * @param name {@code null} for the default name, read after the player's
     * @return the new team's id.
     */
    @Nonnull
    public UUID create(@Nonnull String type, @Nonnull UUID playerId, @Nonnull String playerName, @Nonnull EntityComponents player, @Nullable String name) {
        leaveCurrent(playerId, player, type);

        UUID teamId = teams.create(type, playerId, playerName, name).getId();
        questStore(player).setTeam(type, teamId);
        saveSoon(playerId);
        dispatch(new TeamJoinedEvent(playerId, teamId));
        return teamId;
    }

    /**
     * The player leaves their team of that type, with no copy and no new team of their own, then
     * joins this one and its running quests, whatever their assets say of them.
     *
     * @throws IllegalArgumentException for a team that is gone.
     */
    public void join(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull UUID teamId) {
        TeamRecord team = teams.get(teamId);
        if (team == null) throw new IllegalArgumentException("No team " + teamId);
        if (teamId.equals(questStore(player).getTeams().get(team.getType()))) return;

        // Into the new team first: one emptied meanwhile refuses, leaving the player where they were
        teams.addMember(teamId, playerId);
        leaveCurrent(playerId, player, team.getType());
        enter(playerId, player, team.getType(), teamId);
    }

    /**
     * The player leaves their team of that type and its running quests. For a personal-team type,
     * they go on in a new team of their own holding a copy of those quests and of what the team
     * was handed. A team left empty is disbanded, once the copy is made.
     */
    public void leave(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull EntityComponents player, @Nonnull String type) {
        UUID current = questStore(player).getTeams().get(type);
        if (current == null || isAloneInPersonal(current, type, playerId)) return;

        TeamRecord left = teams.removeMember(current, playerId);
        OpenQuestTeamType teamType = types.get(type);
        UUID own = teamType != null && teamType.isPersonalTeam() ? startOwn(current, type, playerId, playerName) : null;

        exit(playerId, player, type, current, left == null);
        if (own != null) enter(playerId, player, type, own);
    }

    /**
     * Applies the quest side of a change whose documents were written elsewhere. Does nothing when
     * the documents no longer agree, the player having moved again since.
     */
    public void apply(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull TeamChange change) {
        if (change.to() != null) {
            TeamRecord to = teams.get(change.to());
            if (to == null || !to.getMembers().contains(playerId)) return;
        }
        if (change.from() != null) {
            TeamRecord from = teams.get(change.from());
            if (from != null && from.getMembers().contains(playerId)) return;
        }

        UUID held = questStore(player).getTeams().get(change.type());
        if (change.from() != null) exit(playerId, player, change.type(), change.from(), false);

        // The writer read a record older than this session: the team held here was never left
        if (held != null && !held.equals(change.from()) && !held.equals(change.to())) {
            TeamRecord left = teams.removeMember(held, playerId);
            exit(playerId, player, change.type(), held, left == null);
        }
        if (change.to() != null) enter(playerId, player, change.type(), change.to());
    }

    /**
     * Moves any known player into that team, or out of their team of that type for {@code null}.
     * A player online here changes at once on their thread. Any other has the documents and the
     * quests changed now, bound while away like any team member, and is left a message their
     * record takes in when next hosted.
     *
     * @throws IllegalArgumentException for a team that is gone.
     */
    public void change(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull String type, @Nullable UUID to) {
        if (to != null && teams.get(to) == null) throw new IllegalArgumentException("No team " + to);

        if (players.isOnline(playerId)) {
            // Runs on the player's thread after the caller returned: nobody else would see it fail
            players.update(playerId, player -> {
                try {
                    if (to == null) leave(playerId, playerName, player, type);
                    else join(playerId, player, to);
                } catch (RuntimeException e) {
                    LOGGER.atWarning().withCause(e).log("Could not move player %s to team %s", playerId, to);
                }
            });
            return;
        }

        UUID from = absentTeamOf(playerId, type);
        if (Objects.equals(from, to) || (to == null && from != null && isAloneInPersonal(from, type, playerId))) return;

        if (to != null) teams.addMember(to, playerId);
        UUID joined = to;
        if (from != null) {
            TeamRecord left = teams.removeMember(from, playerId);
            OpenQuestTeamType teamType = types.get(type);
            if (to == null && teamType != null && teamType.isPersonalTeam()) joined = startOwn(from, type, playerId, playerName);

            teamQuests.leaveRunningQuests(from, playerId);
            if (left == null) {
                teamQuests.disband(from);
                assignments.deleteHolder(TeamQuestService.key(from));
            }
        }
        if (joined != null) teamQuests.joinRunningQuests(joined, playerId);

        storage.postMessage(PlayerMessage.team(playerId, new TeamChange(type, from, joined)));
    }

    /**
     * @return the player's team of that type: their session's if hosted here, else their record's
     * as the changes waiting for them leave it. {@code null} for none.
     */
    @Nullable
    public UUID currentTeamOf(@Nonnull UUID playerId, @Nonnull String type) {
        QuestStoreComponent hosted = playerState.getQuestStore(playerId);
        return hosted != null ? hosted.getTeams().get(type) : absentTeamOf(playerId, type);
    }

    /**
     * As {@link #wouldLose(UUID, EntityComponents, String)}, for any known player.
     */
    @Nullable
    public TeamRecord wouldLose(@Nonnull UUID playerId, @Nonnull String type) {
        UUID current = currentTeamOf(playerId, type);
        return current == null ? null : losing(current, playerId);
    }

    /**
     * The team their record names, as the changes still waiting for them leave it.
     */
    @Nullable
    private UUID absentTeamOf(@Nonnull UUID playerId, @Nonnull String type) {
        UUID teamId = storage.loadPlayer(playerId).getTeams().get(type);
        for (PlayerMessage message : storage.loadMessages(List.of(playerId))) {
            TeamChange change = message.getTeam();
            if (change != null && change.type().equals(type)) teamId = change.to();
        }
        return teamId;
    }

    /**
     * Leaving on the way to another team: no copy, no team of their own.
     */
    private void leaveCurrent(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull String type) {
        UUID current = questStore(player).getTeams().get(type);
        if (current == null) return;

        TeamRecord left = teams.removeMember(current, playerId);
        exit(playerId, player, type, current, left == null);
    }

    /**
     * Leaving such a team would end in a team of their own with the same quests: kept as it is,
     * rather than deleted, recreated and copied for nothing.
     */
    private boolean isAloneInPersonal(@Nonnull UUID teamId, @Nonnull String type, @Nonnull UUID playerId) {
        OpenQuestTeamType teamType = types.get(type);
        if (teamType == null || !teamType.isPersonalTeam()) return false;

        TeamRecord team = teams.get(teamId);
        return team != null && team.getMembers().equals(List.of(playerId));
    }

    /**
     * Only the quests the player still holds are copied: one they gave up stays given up.
     */
    @Nonnull
    private UUID startOwn(@Nonnull UUID from, @Nonnull String type, @Nonnull UUID playerId, @Nonnull String playerName) {
        UUID own = teams.create(type, playerId, playerName, null).getId();

        for (AbstractQuestProgression<?> quest : teamQuests.getRunningQuests(from)) {
            if (quest.getPlayers().contains(playerId)) teamQuests.addQuest(own, copier.copyFor(quest, playerId).getId());
        }
        assignments.copyHolder(TeamQuestService.key(from), TeamQuestService.key(own));
        return own;
    }

    private void exit(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull String type, @Nonnull UUID teamId, boolean disband) {
        teamQuests.leaveRunningQuests(teamId, playerId);
        if (disband) {
            teamQuests.disband(teamId);
            assignments.deleteHolder(TeamQuestService.key(teamId));
        }
        questStore(player).setTeam(type, null);
        saveSoon(playerId);
        dispatch(new TeamLeftEvent(playerId, teamId));
    }

    private void enter(@Nonnull UUID playerId, @Nonnull EntityComponents player, @Nonnull String type, @Nonnull UUID teamId) {
        questStore(player).setTeam(type, teamId);
        saveSoon(playerId);
        teamQuests.joinRunningQuests(teamId, playerId);
        dispatch(new TeamJoinedEvent(playerId, teamId));
    }

    /**
     * The record is the only way back from a player to their teams: written now, off this thread,
     * so a crash before the save pass cannot leave them in a team that no longer lists them.
     */
    private void saveSoon(@Nonnull UUID playerId) {
        sync.execute("the teams of player " + playerId, () -> playerState.saveNow(playerId));
    }

    @Nonnull
    private QuestStoreComponent questStore(@Nonnull EntityComponents player) {
        return player.ensureAndGetComponent(components.questStore());
    }

    private void dispatch(@Nonnull TeamJoinedEvent event) {
        events.dispatchFor(TeamJoinedEvent.class, event.getPlayerId()).dispatch(event);
    }

    private void dispatch(@Nonnull TeamLeftEvent event) {
        events.dispatchFor(TeamLeftEvent.class, event.getPlayerId()).dispatch(event);
    }

    /**
     * Subscribes to connections and departures. Registered after the player state service, so the
     * record naming the player's teams is read first, and before assignments, so they find every
     * team. A departure runs before that service closes the session it reads.
     */
    public void listen(@Nonnull IEventRegistry events) {
        events.registerGlobal(PlayerConnectEvent.class, event -> {
            UUID playerId = event.getPlayerRef().getUuid();
            try {
                onConnect(playerId, event.getPlayerRef().getUsername(), EntityComponents.of(event.getHolder()));
            } catch (RuntimeException e) {
                // A storage error must not stop the connection; the teams are checked again next time
                LOGGER.atWarning().withCause(e).log("Could not check the teams of connecting player %s", playerId);
            }
        });
        events.registerGlobal(PlayerDisconnectEvent.class, event -> onDisconnect(event.getPlayerRef().getUuid()));
    }

    /**
     * Lets go of the teams of a leaving player that nobody else plays here, so memory and the sync
     * follow the players online rather than every team seen since boot.
     */
    public void onDisconnect(@Nonnull UUID playerId) {
        QuestStoreComponent leaving = playerState.getQuestStore(playerId);
        if (leaving == null) return;

        Set<UUID> stillPlayed = new HashSet<>();
        for (UUID otherId : players.online()) {
            QuestStoreComponent other = otherId.equals(playerId) ? null : playerState.getQuestStore(otherId);
            if (other != null) stillPlayed.addAll(other.getTeams().values());
        }

        for (UUID teamId : leaving.getTeams().values()) {
            if (stillPlayed.contains(teamId)) continue;

            indexes.unload(TeamQuestService.key(teamId));
            assignments.forgetHolder(TeamQuestService.key(teamId));
        }
    }

    /**
     * Applies the team changes left while the player was away, checks the record against the team
     * documents, gives them a team of every personal-team type they have none of, and joins them
     * to their teams' running quests.
     */
    public void onConnect(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull EntityComponents player) {
        // Before the record is checked: a change made while they were away names teams it no longer lists
        playerState.applyTeamChanges(playerId, player);

        QuestStoreComponent questStore = player.ensureAndGetComponent(components.questStore());

        for (Map.Entry<String, UUID> entry : questStore.getTeams().entrySet()) {
            TeamRecord team = teams.get(entry.getValue());
            if (team == null || !team.getMembers().contains(playerId)) questStore.setTeam(entry.getKey(), null);
        }

        for (OpenQuestTeamType type : types.all()) {
            if (type.isPersonalTeam() && !questStore.getTeams().containsKey(type.getId())) {
                questStore.setTeam(type.getId(), teams.create(type.getId(), playerId, playerName, null).getId());
            }
        }
        saveSoon(playerId);

        for (UUID teamId : questStore.getTeams().values()) {
            teamQuests.joinRunningQuests(teamId, playerId);
        }
    }
}
