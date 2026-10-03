package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;
import com.martelstudios.openquests.core.models.QuestScope;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A quest shared by everyone inside the worlds holding it, joined on the way in and left on the
 * way out. Several worlds holding one quest push one progression together.
 */
public class WorldQuestScope extends QuestScope {

    public static final String TYPE = "World";

    public static final BuilderCodec<WorldQuestScope> CODEC = BuilderCodec.builder(WorldQuestScope.class, WorldQuestScope::new, QuestScope.BASE_CODEC)
                                                                          .append(new KeyedCodec<>("Worlds", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new)), (scope, ids) -> scope.worlds.addAll(List.of(ids)), scope -> scope.worlds.toArray(UUID[]::new))
                                                                          .add()
                                                                          .build();

    protected final Set<UUID> worlds = ConcurrentHashMap.newKeySet();

    public WorldQuestScope() {}

    public WorldQuestScope(@Nonnull UUID worldId) {
        worlds.add(worldId);
    }

    /**
     * @return the live set of the worlds holding the quest, closed ones included. A caller changing
     * it marks the quest dirty, so the change is written out.
     */
    @Nonnull
    public Set<UUID> getWorlds() {
        return worlds;
    }

    @Override
    public boolean isReachable() {
        return !WorldQuestService.openWorlds(worlds).isEmpty();
    }

    @Nonnull
    @Override
    public Collection<UUID> getQuestIds() {
        Set<UUID> questIds = new HashSet<>();
        for (World world : WorldQuestService.openWorlds(worlds)) {
            questIds.addAll(WorldQuestService.get().getQuestIds(world));
        }
        return questIds;
    }

    @Override
    public void share(@Nonnull AbstractQuestProgression<?> quest) {
        for (World world : WorldQuestService.openWorlds(worlds)) {
            WorldQuestService.get().addQuest(world, quest.getId());
        }
    }

    @Override
    public void release(@Nonnull AbstractQuestProgression<?> quest) {
        for (World world : WorldQuestService.openWorlds(worlds)) {
            WorldQuestService.get().removeQuest(world, quest.getId());
        }
    }
}
