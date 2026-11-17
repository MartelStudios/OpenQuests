package com.martelstudios.openquests.core.teams;

import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.persistence.QuestStorage;
import com.martelstudios.openquests.core.persistence.TeamChangePersistence;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.services.QuestPlayerStateService;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.sync.QuestSyncService;
import com.martelstudios.openquests.core.utils.Players;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Replays a committed move's quest effects independently of the hosted player's component update. */
final class TeamQuestRecovery {
    private final Teams teams;
    private final TeamQuestService quests;
    private final QuestStorage storage;
    private final QuestAssignmentService assignments;
    private final QuestPlayerStateService playerState;
    private final Players players;
    private final ScopeIndexes indexes;
    private final QuestSyncService sync;

    TeamQuestRecovery(Teams teams, TeamQuestService quests, QuestStorage storage, QuestAssignmentService assignments,
                     QuestPlayerStateService playerState, Players players, ScopeIndexes indexes, QuestSyncService sync) {
        this.teams = teams;
        this.quests = quests;
        this.storage = storage;
        this.assignments = assignments;
        this.playerState = playerState;
        this.players = players;
        this.indexes = indexes;
        this.sync = sync;
    }

    UUID currentTeamOf(UUID player, String type) {
        return storage.loadTeamsForPlayer(player).stream().filter(team -> team.getType().equals(type))
                .map(TeamRecord::getId).findFirst().orElse(null);
    }

    void prepare(UUID player, TeamChange change) {
        UUID current = currentTeamOf(player, change.type());
        boolean currentMove = Objects.equals(current, change.to());
        if (currentMove && current != null) {
            for (AbstractQuestProgression<?> copy : TeamChangePersistence.restore(storage, change)) {
                if (playerState.getQuestStore(player) != null) quests.restoreCopy(copy);
            }
            assignments.forgetHolder(TeamQuestService.key(current));
        }
        for (UUID source : change.sources()) {
            if (source.equals(current)) continue;
            quests.leaveRunningQuests(source, player);
            disbandIfEmpty(source);
        }
        if (currentMove && current != null) quests.joinRunningQuests(current, player);
        Set<UUID> touched = new HashSet<>(change.sources());
        if (change.to() != null) touched.add(change.to());
        sync.execute("teams without a local member", () -> releaseRemoteTeams(touched));
    }

    void reconcile(UUID player, Map<String, UUID> previous, Map<String, UUID> actual, QuestStoreComponent store) {
        for (var entry : previous.entrySet()) {
            if (actual.containsValue(entry.getValue())) continue;
            disbandIfEmpty(entry.getValue());
            store.setTeam(entry.getKey(), null);
        }
        actual.forEach(store::setTeam);
        for (UUID team : actual.values()) quests.joinRunningQuests(team, player, store);
    }

    void onDisconnect(UUID player) {
        QuestStoreComponent leaving = playerState.getQuestStore(player);
        if (leaving == null) return;
        Set<UUID> held = new HashSet<>();
        for (UUID other : players.online()) {
            QuestStoreComponent store = other.equals(player) ? null : playerState.getQuestStore(other);
            if (store != null) held.addAll(store.getTeams().values());
        }
        for (UUID id : leaving.getTeams().values()) if (!held.contains(id)) {
            indexes.unload(TeamQuestService.key(id));
            assignments.forgetHolder(TeamQuestService.key(id));
        }
    }

    private void disbandIfEmpty(UUID id) {
        TeamRecord team = teams.get(id);
        if (team != null && !team.getMembers().isEmpty()) return;
        quests.disband(id);
        assignments.deleteHolder(TeamQuestService.key(id));
        if (team != null) storage.deleteTeam(id, team.getVersion());
    }

    private void releaseRemoteTeams(Set<UUID> touched) {
        Set<UUID> hosted = playerState.getHostedPlayers();
        for (UUID id : touched) {
            TeamRecord team = teams.get(id);
            if (team != null && team.getMembers().stream().anyMatch(member -> players.isOnline(member) || hosted.contains(member))) continue;
            quests.unload(id);
            assignments.forgetHolder(TeamQuestService.key(id));
        }
    }
}
