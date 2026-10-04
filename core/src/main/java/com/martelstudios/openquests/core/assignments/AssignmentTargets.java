package com.martelstudios.openquests.core.assignments;

import com.hypixel.hytale.server.core.universe.world.World;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The holders a scope can hand one occasion of an assignment to. A scope only picks among them:
 * reaching each on the thread allowed to touch it, and sparing one already settled for the
 * period, is done behind.
 */
public interface AssignmentTargets {

    /**
     * The player the occasion concerns, through the components it came with.
     */
    void player(@Nonnull UUID playerId, @Nonnull EntityComponents player);

    /**
     * Every player online, each on the thread of the world they are in.
     */
    void onlinePlayers();

    /**
     * @param joining the player entering that world, who is not among its players until the entry
     * is done, so a quest created on their way in is handed to them here
     */
    void world(@Nonnull World world, @Nullable UUID joining);

    /**
     * @param group the name of the group, the id of the assignment gathering it
     * @param joining the player entering a world of the group, not among its players yet
     */
    void group(@Nonnull String group, @Nullable UUID joining);

    /**
     * @param joining the player still connecting, not online yet for the server to hand a quest
     * created now to them
     */
    void universe(@Nullable UUID joining);
}
