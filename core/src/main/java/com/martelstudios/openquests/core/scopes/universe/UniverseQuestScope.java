package com.martelstudios.openquests.core.scopes.universe;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestScope;
import com.martelstudios.openquests.core.scopes.ScopeServices;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * A quest shared by every player on the server, joined as they connect.
 */
public class UniverseQuestScope extends QuestScope {

    public static final String TYPE = "Universe";

    public static final BuilderCodec<UniverseQuestScope> CODEC = BuilderCodec.builder(UniverseQuestScope.class, UniverseQuestScope::new, QuestScope.BASE_CODEC).build();

    /**
     * The scope holds nothing of its own, so one serves every quest the server shares.
     */
    public static final UniverseQuestScope INSTANCE = new UniverseQuestScope();

    @Override
    public boolean isReachable(@Nonnull ScopeServices services) {
        return true;
    }

    @Nonnull
    @Override
    public Collection<UUID> getQuestIds(@Nonnull ScopeServices services) {
        return List.copyOf(services.universe().getQuestIds());
    }

    @Override
    public void share(@Nonnull ScopeServices services, @Nonnull AbstractQuestProgression<?> quest) {
        quest.apply(new AbstractQuestProgression.SetScope(INSTANCE));
        services.universe().addQuest(quest.getId());
    }

    @Override
    public void release(@Nonnull ScopeServices services, @Nonnull AbstractQuestProgression<?> quest) {
        services.universe().removeQuest(quest.getId());
    }

    @Override
    public void retire(@Nonnull ScopeServices services, @Nonnull AbstractQuestProgression<?> quest) {
        services.universe().unindex(quest.getId());
    }

    /**
     * Every server sharing the storage holds the same universe quests.
     */
    @Override
    public boolean spansServers() {
        return true;
    }

    /**
     * The server outlives any of its worlds.
     */
    @Override
    public boolean outlives(@Nonnull ScopeServices services, @Nonnull UUID closingWorldId) {
        return true;
    }
}
