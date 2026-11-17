package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.event.IEventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.stores.PlayerComponentTypes;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.utils.Players;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Keeps a player's teams and their quests in step: the team documents on one side, the quests
 * the teams share on the other, and the player's record caching which teams they are in.
 */
public class TeamService {

    private final Teams teams;
    private final TeamQuestService teamQuests;
    private final TeamTypes types;
    private final PlayerComponentTypes components;
    private final QuestPlayerStateService playerState;
    private final Players players;
    private final ScopeIndexes indexes;
    private final QuestAssignmentService assignments;

    public TeamService(@Nonnull Teams teams, @Nonnull TeamQuestService teamQuests, @Nonnull TeamTypes types, @Nonnull PlayerComponentTypes components,
                       @Nonnull QuestPlayerStateService playerState, @Nonnull Players players, @Nonnull ScopeIndexes indexes, @Nonnull QuestAssignmentService assignments) {
        this.teams = teams;
        this.teamQuests = teamQuests;
        this.types = types;
        this.components = components;
        this.playerState = playerState;
        this.players = players;
        this.indexes = indexes;
        this.assignments = assignments;
    }

    /**
     * Subscribes to connections and departures. Registered after the player state service, so the
     * record naming the player's teams is read first, and before assignments, so they find every
     * team. A departure runs before that service closes the session it reads.
     */
    public void listen(@Nonnull IEventRegistry events) {
        events.registerGlobal(PlayerConnectEvent.class, event -> onConnect(event.getPlayerRef().getUuid(), event.getPlayerRef().getUsername(), EntityComponents.of(event.getHolder())));
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
     * Checks the record against the team documents, gives the player a team of every personal-team
     * type they have none of, and joins them to their teams' running quests.
     */
    public void onConnect(@Nonnull UUID playerId, @Nonnull String playerName, @Nonnull EntityComponents player) {
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

        for (UUID teamId : questStore.getTeams().values()) {
            teamQuests.joinRunningQuests(teamId, playerId);
        }
    }
}
