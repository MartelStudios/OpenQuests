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
 * way out. Several worlds holding one quest push one progression together, and it goes on as long
 * as one of them is open.
 */
public class WorldQuestScope extends QuestScope {

    public static final String TYPE = "World";

    public static final BuilderCodec<WorldQuestScope> CODEC = BuilderCodec.builder(WorldQuestScope.class, WorldQuestScope::new, QuestScope.BASE_CODEC)
                                                                          .append(new KeyedCodec<>("Worlds", new ArrayCodec<>(Codec.UUID_STRING, UUID[]::new)), (scope, ids) -> scope.worlds.addAll(List.of(ids)), scope -> scope.worlds.toArray(UUID[]::new))
                                                                          .add()
                                                                          .build();

    protected final Set<UUID> worlds = ConcurrentHashMap.newKeySet();

    public WorldQuestScope() {}

    public WorldQuestScope(@Nonnull Collection<UUID> worldIds) {
        worlds.addAll(worldIds);
    }

    /**
     * @return the worlds holding the quest, closed ones included.
     */
    @Nonnull
    public Set<UUID> getWorlds() {
        return Set.copyOf(worlds);
    }

    @Override
    public boolean isReachable() {
        return !WorldQuestService.get().openWorlds(worlds).isEmpty();
    }

    @Nonnull
    @Override
    public Collection<UUID> getQuestIds() {
        Set<UUID> questIds = new HashSet<>();
        for (World world : WorldQuestService.get().openWorlds(worlds)) {
            questIds.addAll(WorldQuestService.get().getQuestIds(world));
        }
        return questIds;
    }

    /**
     * Into every world of this scope still open, each on its own thread.
     */
    @Override
    public void share(@Nonnull AbstractQuestProgression<?> quest) {
        List<World> open = WorldQuestService.get().openWorlds(worlds);

        quest.setScope(new WorldQuestScope(open.stream().map(world -> world.getWorldConfig().getUuid()).toList()));
        for (World world : open) {
            WorldQuestService.get().addQuest(world, quest.getId());
        }
    }

    @Override
    public void release(@Nonnull AbstractQuestProgression<?> quest) {
        for (World world : WorldQuestService.get().openWorlds(worlds)) {
            WorldQuestService.get().removeQuest(world, quest.getId());
        }
    }

    @Override
    public void retire(@Nonnull AbstractQuestProgression<?> quest) {
        for (World world : WorldQuestService.get().openWorlds(worlds)) {
            WorldQuestService.get().unindex(world, quest.getId());
        }
    }

    /**
     * A world is its own holder: the quest goes on only while another world holding it is open.
     */
    @Override
    public boolean outlives(@Nonnull UUID closingWorldId) {
        for (World world : WorldQuestService.get().openWorlds(worlds)) {
            if (!world.getWorldConfig().getUuid().equals(closingWorldId)) return true;
        }
        return false;
    }

    @Override
    public boolean addWorld(@Nonnull UUID worldId) {
        return worlds.add(worldId);
    }

    @Override
    public boolean removeWorld(@Nonnull UUID worldId) {
        return worlds.remove(worldId);
    }
}
