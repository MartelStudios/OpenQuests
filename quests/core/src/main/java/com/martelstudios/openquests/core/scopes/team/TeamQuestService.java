package com.martelstudios.openquests.core.scopes.team;

import com.hypixel.hytale.logger.HytaleLogger;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.AbstractCompositeQuestProgression;
import com.martelstudios.openquests.core.persistence.TeamRecord;
import com.martelstudios.openquests.core.scopes.ScopeIndexes;
import com.martelstudios.openquests.core.scopes.player.PlayerQuestService;
import com.martelstudios.openquests.core.services.QuestProgressionService;
import com.martelstudios.openquests.core.teams.Teams;
import com.martelstudios.openquests.core.utils.Players;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

/**
 * Holds the quests each team shares: indexes them under the team's key and binds its members to
 * them, every member when a quest is shared and each one as they connect. Who is a member is read
 * from the team's document at each use, never kept here.
 *
 * <p>A member joins the team's quests past the constraints of their assets: the team holds them
 * already, and a member left out would split the team.
 */
public class TeamQuestService {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * The prefix of every team's quest index, followed by the servers sharing the storage.
     */
    public static final String TEAM_INDEX_PREFIX = "team:";

    private final ScopeIndexes indexes;
    private final QuestProgressionService progression;
    private final Players players;
    private final Teams teams;
    private final PlayerQuestService playerQuests;

    /** Restores a persisted move's fixed copy once; replay keeps the current progression already loaded. */
    public void restoreCopy(@Nonnull AbstractQuestProgression<?> copy) {
        if (progression.getQuest(copy.getId()) == null) progression.registerLoadedQuest(copy);
    }

    /** Releases an already loaded team's index and quest trees once nobody here hosts a member. */
    public void unload(@Nonnull UUID teamId) {
        if (!indexes.getLoadedKeys(TEAM_INDEX_PREFIX).contains(key(teamId))) return;
        List<AbstractQuestProgression<?>> held = List.copyOf(indexes.resolve(key(teamId)));
        indexes.unload(key(teamId));
        for (AbstractQuestProgression<?> quest : held) if (!isHeldOnline(quest)) unloadTree(quest);
    }

    private boolean isHeldOnline(AbstractQuestProgression<?> quest) {
        if (quest.getPlayers().stream().anyMatch(players::isOnline) || quest.getAbandonedPlayers().stream().anyMatch(players::isOnline)) return true;
        return quest instanceof AbstractCompositeQuestProgression<?> composite && composite.getChildren().stream().anyMatch(this::isHeldOnline);
    }

    private void unloadTree(AbstractQuestProgression<?> quest) {
        if (quest instanceof AbstractCompositeQuestProgression<?> composite) {
            for (AbstractQuestProgression<?> child : composite.getChildren()) unloadTree(child);
        }
        progression.unloadQuest(quest.getId());
    }

    /** Uses committed team membership even if a move's quest binding is still waiting to be recovered. */
    @Nonnull
    public Set<UUID> completionPlayers(@Nonnull AbstractQuestProgression<?> quest) {
        AbstractQuestProgression<?> root = quest;
        Set<UUID> visited = new HashSet<>();
        while (root.getParentId() != null && visited.add(root.getId())) {
            AbstractQuestProgression<?> parent = progression.loadQuest(root.getParentId());
            if (parent == null) break;
            root = parent;
        }
        if (!(root.getScope() instanceof TeamQuestScope scope)) return Set.copyOf(quest.getPlayers());
        TeamRecord team = teams.get(scope.getTeamId());
        if (team == null) return Set.of();
        Set<UUID> recipients = new HashSet<>(team.getMembers());
        recipients.removeAll(quest.getAbandonedPlayers());
        return recipients;
    }

    public TeamQuestService(@Nonnull ScopeIndexes indexes, @Nonnull QuestProgressionService progression, @Nonnull Players players, @Nonnull Teams teams, @Nonnull PlayerQuestService playerQuests) {
        this.indexes = indexes;
        this.progression = progression;
        this.players = players;
        this.teams = teams;
        this.playerQuests = playerQuests;
    }

    /**
     * @return {@code team:<uuid>}, the key of the team's running quests and assignment records.
     */
    @Nonnull
    public static String key(@Nonnull UUID teamId) {
        return TEAM_INDEX_PREFIX + teamId;
    }

    /**
     * @return the team that key names, {@code null} for the key of anything else.
     */
    @Nullable
    public static UUID parseKey(@Nonnull String key) {
        if (!key.startsWith(TEAM_INDEX_PREFIX)) return null;

        try {
            return UUID.fromString(key.substring(TEAM_INDEX_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * @return the live ids of the team's running quests.
     */
    @Nonnull
    public Set<UUID> getQuestIds(@Nonnull UUID teamId) {
        return indexes.getIds(key(teamId));
    }

    /**
     * Shares the quest with the team: scope written, indexed, and every member bound, online or
     * not, so the absent are paid and counted like the others.
     */
    public void addQuest(@Nonnull UUID teamId, @Nonnull UUID questId) {
        TeamRecord team = teams.get(teamId);
        if (team != null) {
            addQuest(teamId, questId, team.getMembers());
            return;
        }

        // Handed out from a stale session: nobody would ever play it
        AbstractQuestProgression<?> quest = progression.getQuest(questId);
        if (quest != null) progression.unregisterQuest(quest);
    }

    /**
     * As {@link #addQuest(UUID, UUID)}, for a caller who knows the members: one read less per quest.
     */
    public void addQuest(@Nonnull UUID teamId, @Nonnull UUID questId, @Nonnull Collection<UUID> members) {
        AbstractQuestProgression<?> quest = progression.loadQuest(questId);
        if (quest == null) return;

        // Before the index lists it, which writes the quest: another server reading it finds it shared
        if (!isSharedBy(quest, teamId)) quest.apply(new AbstractQuestProgression.SetScope(new TeamQuestScope(teamId)));

        if (!indexes.add(key(teamId), questId)) return;

        LOGGER.atInfo().log("Added quest %s to team %s", questId, teamId);

        for (UUID memberId : members) {
            progression.joinQuest(quest, memberId, false);
        }
    }

    /**
     * Takes the quest away from the team, its scope cleared while it is still there to write on.
     */
    public void removeQuest(@Nonnull UUID teamId, @Nonnull UUID questId) {
        indexes.remove(key(teamId), questId);

        AbstractQuestProgression<?> quest = progression.getQuest(questId);
        if (quest != null && isSharedBy(quest, teamId)) quest.apply(new AbstractQuestProgression.SetScope(null));
    }

    /**
     * Takes an ended quest off the team's running ones; it keeps its scope and its players.
     */
    public void unindex(@Nonnull UUID teamId, @Nonnull UUID questId) {
        indexes.remove(key(teamId), questId);
    }

    /**
     * Brings the quests another server shared with the team to the members online here. That
     * server bound them already, so no event told this one: their store takes the quest directly.
     */
    public void joinAdded(@Nonnull UUID teamId, @Nonnull Collection<UUID> questIds) {
        TeamRecord team = teams.get(teamId);
        if (team == null) return;

        for (UUID questId : questIds) {
            AbstractQuestProgression<?> quest = progression.loadQuest(questId);
            if (quest == null) continue;

            for (UUID memberId : team.getMembers()) {
                if (!players.isOnline(memberId)) continue;

                if (quest.getPlayers().contains(memberId)) playerQuests.addQuestToPlayerStore(quest, memberId);
                else progression.joinQuest(quest, memberId, false);
            }
        }
    }

    /**
     * @return the team's quests still running, read back if need be.
     */
    @Nonnull
    public List<AbstractQuestProgression<?>> getRunningQuests(@Nonnull UUID teamId) {
        return indexes.resolve(key(teamId)).stream().filter(quest -> !quest.isOver()).toList();
    }

    /**
     * Takes a member leaving the team off its running quests, without them giving the quests up.
     */
    public void leaveRunningQuests(@Nonnull UUID teamId, @Nonnull UUID playerId) {
        for (AbstractQuestProgression<?> quest : getRunningQuests(teamId)) {
            quest.removePlayer(playerId);
        }
    }

    /**
     * Has a member join the team's running quests, a connecting one included, and drops from the
     * index the quests found over.
     */
    public void joinRunningQuests(@Nonnull UUID teamId, @Nonnull UUID playerId) {
        joinRunningQuests(teamId, playerId, null);
    }

    /** Uses the incoming holder at connection, before the universe necessarily lists the player as online. */
    public void joinRunningQuests(@Nonnull UUID teamId, @Nonnull UUID playerId, @Nullable QuestStoreComponent connectingStore) {
        for (AbstractQuestProgression<?> quest : indexes.resolve(key(teamId))) {
            if (quest.isOver()) indexes.remove(key(teamId), quest.getId());
            else {
                if (!quest.getPlayers().contains(playerId)) progression.joinQuest(quest, playerId, false);
                if (connectingStore != null) playerQuests.addQuestToPlayerStore(connectingStore, quest, playerId);
                else playerQuests.addQuestToPlayerStore(quest, playerId);
            }
        }
    }

    /**
     * Ends the team's share of quests: its running quests unregistered and its index deleted. Its
     * assignment records are the caller's to delete, through the assignment service caching them.
     */
    public void disband(@Nonnull UUID teamId) {
        for (AbstractQuestProgression<?> quest : List.copyOf(indexes.resolve(key(teamId)))) {
            progression.unregisterQuest(quest);
        }
        indexes.delete(key(teamId));
    }

    private static boolean isSharedBy(@Nonnull AbstractQuestProgression<?> quest, @Nonnull UUID teamId) {
        return quest.getScope() instanceof TeamQuestScope scope && teamId.equals(scope.getTeamId());
    }
}
