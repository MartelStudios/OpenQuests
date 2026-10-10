package com.martelstudios.openquests.core.scopes.world;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.models.AbstractQuestProgression;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * A quest shared by a whole group of worlds, every world one assignment gathers: one progression
 * pushed from all of them, taken up by each as it is entered. The worlds it has reached so far are
 * kept like any world quest's; the group is what reaches the next ones.
 */
public class WorldsQuestScope extends WorldQuestScope {

    public static final String TYPE = "Worlds";

    public static final BuilderCodec<WorldsQuestScope> CODEC = BuilderCodec.builder(WorldsQuestScope.class, WorldsQuestScope::new, WorldQuestScope.CODEC)
                                                                           .append(new KeyedCodec<>("Group", Codec.STRING), (scope, group) -> scope.group = group, scope -> scope.group)
                                                                           .add()
                                                                           .build();

    protected String group;

    public WorldsQuestScope() {}

    public WorldsQuestScope(@Nonnull String group) {
        this.group = group;
    }

    /**
     * @return the name of the group, which is the id of the assignment gathering it.
     */
    @Nonnull
    public String getGroup() {
        return group;
    }

    /**
     * A group is there to share with even while none of its worlds is open.
     */
    @Override
    public boolean isReachable() {
        return true;
    }

    @Nonnull
    @Override
    public Collection<UUID> getQuestIds() {
        return List.copyOf(WorldGroupIndex.get().getQuestIds(group));
    }

    /**
     * Into the group, and into each of its worlds someone is in, on their own thread; the others
     * take it up as they are entered.
     */
    @Override
    public void share(@Nonnull AbstractQuestProgression<?> quest) {
        quest.setScope(new WorldsQuestScope(group));
        WorldGroupIndex.get().add(group, quest.getId());

        for (World world : WorldGroupIndex.get().getJoinedWorlds(group)) {
            WorldQuestService.get().addQuest(world, quest.getId());
        }
    }

    @Override
    public void release(@Nonnull AbstractQuestProgression<?> quest) {
        WorldGroupIndex.get().remove(group, quest.getId());
        super.release(quest);
    }

    @Override
    public void retire(@Nonnull AbstractQuestProgression<?> quest) {
        WorldGroupIndex.get().remove(group, quest.getId());
        super.retire(quest);
    }

    /**
     * The worlds of a group may run on several servers sharing the storage.
     */
    @Override
    public boolean spansServers() {
        return true;
    }

    /**
     * The group is the holder, and a group never closes: its quests wait for the next of its worlds
     * to be entered, and end on their own terms or as their assignment replaces them.
     */
    @Override
    public boolean outlives(@Nonnull UUID closingWorldId) {
        return true;
    }
}
