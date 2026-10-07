package com.martelstudios.openquests.core.scopes.universe;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestScope;

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
    public boolean isReachable() {
        return true;
    }

    @Nonnull
    @Override
    public Collection<UUID> getQuestIds() {
        return List.copyOf(UniverseQuestService.get().getQuestIds());
    }

    @Override
    public void share(@Nonnull AbstractQuestProgression<?> quest) {
        quest.setScope(INSTANCE);
        UniverseQuestService.get().addQuest(quest.getId());
    }

    @Override
    public void release(@Nonnull AbstractQuestProgression<?> quest) {
        UniverseQuestService.get().removeQuest(quest.getId());
    }

    @Override
    public void retire(@Nonnull AbstractQuestProgression<?> quest) {
        UniverseQuestService.get().unindex(quest.getId());
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
    public boolean outlives(@Nonnull UUID closingWorldId) {
        return true;
    }
}
