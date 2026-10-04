package com.martelstudios.openquests.core.assignments.scope;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.martelstudios.openquests.core.assignments.Occasion;
import com.martelstudios.openquests.core.assignments.OpenQuestAssignment;
import com.martelstudios.openquests.core.assignments.PlayerAssignmentHolder;
import com.martelstudios.openquests.core.assignments.QuestAssignmentService;
import com.martelstudios.openquests.core.utils.EntityComponents;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * A quest of their own for the player the occasion concerns, theirs to keep wherever they go. An
 * occasion concerning nobody in particular, a schedule, concerns every player online.
 */
public class PlayerAssignmentScope extends AssignmentScope {

    public static final String TYPE = "Player";

    public static final BuilderCodec<PlayerAssignmentScope> CODEC = BuilderCodec.builder(PlayerAssignmentScope.class, PlayerAssignmentScope::new, AssignmentScope.BASE_CODEC).build();

    /**
     * A player met on their way in is written through their holder, on the thread the occasion
     * came on; the others on their own world's thread, where their components may be touched.
     */
    @Override
    public void reach(@Nonnull OpenQuestAssignment assignment, @Nonnull Occasion occasion) {
        QuestAssignmentService service = QuestAssignmentService.get();
        String key = occasionKey(occasion);

        if (occasion.getPlayerId() != null && occasion.getPlayer() != null) {
            service.offerAll(assignment, service.playerHolder(occasion.getPlayerId(), occasion.getPlayer()), key, occasion.isTimed());
            return;
        }

        if (occasion.getPlayerId() != null) return;

        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            UUID playerId = playerRef.getUuid();
            if (service.isSettled(PlayerAssignmentHolder.keyOf(playerId), assignment, key)) continue;

            EntityComponents.update(playerId, player -> service.offerAll(assignment, service.playerHolder(playerId, player), key, occasion.isTimed()));
        }
    }

    /**
     * The player moves, so the world they enter is part of what tells hand-outs apart.
     *
     * @return {@code world:<uuid>} for an entry, the occasion's time otherwise.
     */
    @Nonnull
    public static String occasionKey(@Nonnull Occasion occasion) {
        if (occasion.getWorld() != null) return "world:" + occasion.getWorld().getWorldConfig().getUuid();
        return timeKey(occasion);
    }
}
