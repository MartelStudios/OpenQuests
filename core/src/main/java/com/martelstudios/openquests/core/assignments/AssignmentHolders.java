package com.martelstudios.openquests.core.assignments;

import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestScope;
import com.martelstudios.openquests.core.scopes.universe.UniverseQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldGroupIndex;
import com.martelstudios.openquests.core.scopes.world.WorldQuestScope;
import com.martelstudios.openquests.core.scopes.world.WorldQuestService;
import com.martelstudios.openquests.core.scopes.world.WorldsQuestScope;
import com.martelstudios.openquests.core.stores.QuestStoreComponent;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Builds each kind of holder and names it: a player, whose records travel with their own, or a
 * world, a group of worlds or the server, whose records live in the shared store and whose quests
 * are shared through the scope of that kind. A holder's key is the same as its index's.
 */
public final class AssignmentHolders {

    @Nonnull
    private final SharedAssignmentRecords records;

    public AssignmentHolders(@Nonnull SharedAssignmentRecords records) {
        this.records = records;
    }

    /**
     * @return {@code player:<uuid>}.
     */
    @Nonnull
    public static String playerKey(@Nonnull UUID playerId) {
        return PlayerAssignmentHolder.keyOf(playerId);
    }

    /**
     * @return {@code world:<uuid>}.
     */
    @Nonnull
    public static String worldKey(@Nonnull World world) {
        return WorldQuestService.indexKey(world);
    }

    /**
     * @return {@code worlds:<group>}.
     */
    @Nonnull
    public static String groupKey(@Nonnull String group) {
        return WorldGroupIndex.keyOf(group);
    }

    /**
     * @return {@code universe}.
     */
    @Nonnull
    public static String universeKey() {
        return UniverseQuestService.UNIVERSE_INDEX_KEY;
    }

    /**
     * @param player the player's components, written on the thread the occasion came on
     */
    @Nonnull
    public AssignmentHolder player(@Nonnull UUID playerId, @Nonnull EntityComponents player) {
        return new PlayerAssignmentHolder(playerId, player.ensureAndGetComponent(QuestStoreComponent.getComponentType()));
    }

    /**
     * @param joining the player entering that world, handed what is created on their way in
     */
    @Nonnull
    public AssignmentHolder world(@Nonnull World world, @Nullable UUID joining) {
        return new SharedAssignmentHolder(records, worldKey(world), new WorldQuestScope(List.of(world.getWorldConfig().getUuid())), joining);
    }

    /**
     * @param joining the player entering a world of the group, handed what is created on their way in
     */
    @Nonnull
    public AssignmentHolder group(@Nonnull String group, @Nullable UUID joining) {
        return new SharedAssignmentHolder(records, groupKey(group), new WorldsQuestScope(group), joining);
    }

    /**
     * @param joining the player still connecting, handed what is created on their way in
     */
    @Nonnull
    public AssignmentHolder universe(@Nullable UUID joining) {
        return new SharedAssignmentHolder(records, universeKey(), UniverseQuestScope.INSTANCE, joining);
    }
}
