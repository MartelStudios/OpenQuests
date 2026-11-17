package com.martelstudios.openquests.core.teams;

import com.hypixel.hytale.event.IEventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.scopes.team.TeamQuestService;
import com.martelstudios.openquests.core.stores.PlayerComponentTypes;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.Map;
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

    public TeamService(@Nonnull Teams teams, @Nonnull TeamQuestService teamQuests, @Nonnull TeamTypes types, @Nonnull PlayerComponentTypes components) {
        this.teams = teams;
        this.teamQuests = teamQuests;
        this.types = types;
        this.components = components;
    }

    /**
     * Subscribes to connections. Registered after the player state service, so the record naming
     * the player's teams is read first, and before assignments, so they find every team.
     */
    public void listen(@Nonnull IEventRegistry events) {
        events.registerGlobal(PlayerConnectEvent.class, event -> onConnect(event.getPlayerRef().getUuid(), event.getPlayerRef().getUsername(), EntityComponents.of(event.getHolder())));
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
