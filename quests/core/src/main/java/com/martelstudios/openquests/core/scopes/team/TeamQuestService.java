package com.martelstudios.openquests.core.scopes.team;

import com.hypixel.hytale.event.IEventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.stores.PlayerComponentTypes;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.teams.OpenQuestTeamType;
import com.martelstudios.openquests.core.teams.TeamKey;
import com.martelstudios.openquests.core.utils.EntityComponents;
import com.martelstudios.openquests.core.utils.Players;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Holds the quests each team shares: indexes them under the team's key, hands them to its online
 * members and to members connecting later. Members are listed under a key of the team's own, but
 * for a personal team, whose name gives its only member.
 */
public class TeamQuestService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * The prefix of every team's quest index, followed by the servers sharing the storage.
     */
    public static final String TEAM_INDEX_PREFIX = "team:";

    /**
     * The prefix of every team's members index.
     */
    public static final String MEMBERS_INDEX_PREFIX = "team-members:";

    private final ScopeIndexes indexes;
    private final QuestProgressionService progression;
    private final Players players;
    private final PlayerComponentTypes components;
    private final Supplier<Collection<OpenQuestTeamType>> teamTypes;

    public TeamQuestService(@Nonnull ScopeIndexes indexes, @Nonnull QuestProgressionService progression, @Nonnull Players players, @Nonnull PlayerComponentTypes components, @Nonnull Supplier<Collection<OpenQuestTeamType>> teamTypes) {
        this.indexes = indexes;
        this.progression = progression;
        this.players = players;
        this.components = components;
        this.teamTypes = teamTypes;
    }

    /**
     * Subscribes to connections, which bring the player's teams' quests to them. Registered after
     * the player state service, so the record naming their teams is read first.
     */
    public void listen(@Nonnull IEventRegistry events) {
        events.registerGlobal(PlayerConnectEvent.class, event -> joinRunningQuests(event.getPlayerRef().getUuid(), EntityComponents.of(event.getHolder())));
    }

    /**
     * @return the live ids of the team's running quests.
     */
    @Nonnull
    public Set<UUID> getQuestIds(@Nonnull TeamKey team) {
        return indexes.getIds(team.holderKey());
    }

    /**
     * Shares the quest with the team: scope written, indexed, joined by the online members.
     */
    public void addQuest(@Nonnull TeamKey team, @Nonnull UUID questId) {
        AbstractQuestProgression<?> quest = progression.loadQuest(questId);
        if (quest == null) return;

        // Before the index lists it, which writes the quest: another server reading it finds it shared
        if (!isSharedBy(quest, team)) quest.apply(new AbstractQuestProgression.SetScope(new TeamQuestScope(team)));

        if (!indexes.add(team.holderKey(), questId)) return;

        LOGGER.atInfo().log("Added quest %s to team %s", questId, team.holderKey());

        joinOnlineMembers(team, quest);
    }

    /**
     * Takes the quest away from the team, its scope cleared while it is still there to write on.
     */
    public void removeQuest(@Nonnull TeamKey team, @Nonnull UUID questId) {
        indexes.remove(team.holderKey(), questId);

        AbstractQuestProgression<?> quest = progression.getQuest(questId);
        if (quest != null && isSharedBy(quest, team)) quest.apply(new AbstractQuestProgression.SetScope(null));
    }

    /**
     * Takes an ended quest off the team's running ones; it keeps its scope and its players.
     */
    public void unindex(@Nonnull TeamKey team, @Nonnull UUID questId) {
        indexes.remove(team.holderKey(), questId);
    }

    /**
     * @return the team's members, online or not.
     */
    @Nonnull
    public Set<UUID> members(@Nonnull TeamKey team) {
        UUID owner = team.owner();
        if (owner != null) return Set.of(owner);

        return Set.copyOf(indexes.getIds(team.membersKey()));
    }

    /**
     * Lists the player as a member. A personal team lists nobody: its name says who.
     */
    public void addMember(@Nonnull TeamKey team, @Nonnull UUID playerId) {
        if (!team.isPersonal()) indexes.add(team.membersKey(), playerId);
    }

    /**
     * Takes the player off the team's members.
     */
    public void removeMember(@Nonnull TeamKey team, @Nonnull UUID playerId) {
        if (!team.isPersonal()) indexes.remove(team.membersKey(), playerId);
    }

    /**
     * @return the teams the player is in: those their record names, and their personal team of
     * every personal-team type it names none of.
     */
    @Nonnull
    public List<TeamKey> teamsOf(@Nonnull UUID playerId, @Nonnull EntityComponents player) {
        QuestStoreComponent questStore = player.getComponent(components.questStore());
        Map<String, String> named = questStore == null ? Map.of() : questStore.getTeams();

        List<TeamKey> teams = new ArrayList<>();
        named.forEach((type, name) -> teams.add(new TeamKey(type, name)));

        for (OpenQuestTeamType type : teamTypes.get()) {
            if (type.isPersonalTeam() && !named.containsKey(type.getId())) teams.add(TeamKey.personal(type.getId(), playerId));
        }
        return teams;
    }

    /**
     * Has the team's members online here join the quests another server shared with it.
     */
    public void joinAdded(@Nonnull TeamKey team, @Nonnull Collection<UUID> questIds) {
        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = progression.loadQuest(questId);
            if (quest != null) joinOnlineMembers(team, quest);
        }
    }

    /**
     * Has a connecting player join the running quests of every team they are in, through the
     * components handed in: they are not online yet.
     */
    public void joinRunningQuests(@Nonnull UUID playerId, @Nonnull EntityComponents player) {
        for (TeamKey team : teamsOf(playerId, player)) {
            for (AbstractQuestProgression<?> quest : indexes.resolve(team.holderKey())) {
                progression.joinQuest(quest, playerId);
            }
        }
    }

    private void joinOnlineMembers(@Nonnull TeamKey team, @Nonnull AbstractQuestProgression<?> quest) {
        for (UUID playerId : members(team)) {
            if (players.isOnline(playerId)) progression.joinQuest(quest, playerId);
        }
    }

    private static boolean isSharedBy(@Nonnull AbstractQuestProgression<?> quest, @Nonnull TeamKey team) {
        return quest.getScope() instanceof TeamQuestScope scope && team.equals(scope.getTeam());
    }
}
