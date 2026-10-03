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

    @Override
    public boolean isReachable() {
        return true;
    }

    @Nonnull
    @Override
    public Collection<UUID> getQuestIds() {
        return List.copyOf(UniverseQuestService.get().getQuests().getAllIds());
    }

    @Override
    public void share(@Nonnull AbstractQuestProgression<?> quest) {
        UniverseQuestService.get().addQuest(quest.getId());
    }

    @Override
    public void release(@Nonnull AbstractQuestProgression<?> quest) {
        UniverseQuestService.get().removeQuest(quest.getId());
    }
}
