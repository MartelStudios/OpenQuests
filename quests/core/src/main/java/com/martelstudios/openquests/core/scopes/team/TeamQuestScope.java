package com.martelstudios.openquests.core.scopes.team;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.scopes.ScopeServices;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * A quest shared by every member of one team, online or not, joined by members as they connect.
 */
public class TeamQuestScope extends QuestScope {

    public static final String TYPE = "Team";

    public static final BuilderCodec<TeamQuestScope> CODEC = BuilderCodec.builder(TeamQuestScope.class, TeamQuestScope::new, QuestScope.BASE_CODEC)
                                                                         .append(new KeyedCodec<>("Team", Codec.UUID_STRING), (scope, teamId) -> scope.teamId = teamId, scope -> scope.teamId)
                                                                         .addValidator(Validators.nonNull())
                                                                         .documentation("UUID of the team that shares the quest.")
                                                                         .add()
                                                                         .documentation("Identifies a quest shared by a team.")
                                                                         .build();

    protected UUID teamId;

    protected TeamQuestScope() {}

    public TeamQuestScope(@Nonnull UUID teamId) {
        this.teamId = teamId;
    }

    /**
     * @return the id of the team sharing the quest.
     */
    @Nonnull
    public UUID getTeamId() {
        return teamId;
    }

    /**
     * A team is there to share with even while none of its members is online.
     */
    @Override
    public boolean isReachable(@Nonnull ScopeServices services) {
        return true;
    }

    @Nonnull
    @Override
    public Collection<UUID> getQuestIds(@Nonnull ScopeServices services) {
        return List.copyOf(services.teams().getQuestIds(teamId));
    }

    @Override
    public void share(@Nonnull ScopeServices services, @Nonnull AbstractQuestProgression<?> quest) {
        quest.apply(new AbstractQuestProgression.SetScope(new TeamQuestScope(teamId)));
        services.teams().addQuest(teamId, quest.getId());
    }

    @Override
    public void release(@Nonnull ScopeServices services, @Nonnull AbstractQuestProgression<?> quest) {
        services.teams().removeQuest(teamId, quest.getId());
    }

    @Override
    public void retire(@Nonnull ScopeServices services, @Nonnull AbstractQuestProgression<?> quest) {
        services.teams().unindex(teamId, quest.getId());
    }

    /**
     * Members play on every server sharing the storage.
     */
    @Override
    public boolean spansServers() {
        return true;
    }

    /**
     * A team belongs to no world.
     */
    @Override
    public boolean outlives(@Nonnull ScopeServices services, @Nonnull UUID closingWorldId) {
        return true;
    }
}
